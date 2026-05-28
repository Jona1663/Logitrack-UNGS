package com.logitrack.sistema_logistica.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.logitrack.sistema_logistica.dto.AlertaListadoDTO;
import com.logitrack.sistema_logistica.dto.IncidenciaDTO;
import com.logitrack.sistema_logistica.dto.ResolverIncidenciaDTO;
import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.Incidencia;
import com.logitrack.sistema_logistica.model.Persona;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.EstadoIncidencia;
import com.logitrack.sistema_logistica.model.enums.TipoIncidencia;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.IncidenciaRepository;

@ExtendWith(MockitoExtension.class)
public class IncidenciaServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private TrackingGeospatialService trackingService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private IncidenciaService incidenciaService;

    private Envio envioValido;
    private IncidenciaDTO incidenciaDTO;

    @BeforeEach
    void setUp() {
        // Preparamos datos básicos que usaremos en varios tests
        envioValido = new Envio();
        envioValido.setIdEnvio("LT-1000");
        envioValido.setEstadoActual(EstadoEnvio.EN_TRANSITO);

        incidenciaDTO = new IncidenciaDTO();
        incidenciaDTO.setTipoIncidencia(TipoIncidencia.MECANICA);
        incidenciaDTO.setDescripcion("Neumático pinchado");

        // --- Simulamos el usuario logueado ---
        org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        org.springframework.security.core.Authentication authentication = mock(org.springframework.security.core.Authentication.class);
        
        // Le agregamos lenient() para que Mockito no tire error si el test no usa esta simulación
        org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn("chofer_test"); 
        
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
    }
    

    // =========================================================================
    // TESTS PARA: reportarIncidencia (US 32)
    // =========================================================================

    @Test
    void reportarIncidencia_CaminoFeliz_GuardaYNotifica() {
        // Arrange
        when(envioRepository.findById("LT-1000")).thenReturn(Optional.of(envioValido));
        
        Map<String, Object> ubicacionMock = new HashMap<>();
        ubicacionMock.put("latitudActual", -34.6037);
        ubicacionMock.put("longitudActual", -58.3816);
        when(trackingService.calcularUbicacionInterpolada(envioValido)).thenReturn(ubicacionMock);

        // Act
        incidenciaService.reportarIncidencia("LT-1000", incidenciaDTO);

        // Assert
        ArgumentCaptor<Incidencia> incidenciaCaptor = ArgumentCaptor.forClass(Incidencia.class);
        verify(incidenciaRepository).save(incidenciaCaptor.capture());
        
        Incidencia incidenciaGuardada = incidenciaCaptor.getValue();
        assertEquals(TipoIncidencia.MECANICA, incidenciaGuardada.getTipoIncidencia());
        assertEquals(EstadoIncidencia.PENDIENTE, incidenciaGuardada.getEstado());
        assertTrue(incidenciaGuardada.getLugarIncidencia().contains("-34.6037")); // Verificamos ubicación
        
        verify(notificationService).enviarNotificacion(anyString(), anyString(), anyString());
    }

    @Test
    void reportarIncidencia_CaminoTriste_EnvioNoExiste_LanzaException() {
        // Arrange
        when(envioRepository.findById("LT-9999")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            incidenciaService.reportarIncidencia("LT-9999", incidenciaDTO);
        });
        
        assertEquals("Envío no encontrado", exception.getMessage());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void reportarIncidencia_CaminoTriste_EstadoInvalido_LanzaException() {
        // Arrange
        envioValido.setEstadoActual(EstadoEnvio.ENTREGADO); // Estado cerrado
        when(envioRepository.findById("LT-1000")).thenReturn(Optional.of(envioValido));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            incidenciaService.reportarIncidencia("LT-1000", incidenciaDTO);
        });

        assertEquals("Solo se pueden reportar incidencias sobre viajes en curso.", exception.getMessage());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void reportarIncidencia_CaminoAlterno_FallaTracking_GuardaIgual() {
        // Arrange
        when(envioRepository.findById("LT-1000")).thenReturn(Optional.of(envioValido));
        when(trackingService.calcularUbicacionInterpolada(envioValido))
            .thenThrow(new RuntimeException("Error geométrico")); // Simulamos caída del mapa

        // Act
        incidenciaService.reportarIncidencia("LT-1000", incidenciaDTO);

        // Assert
        ArgumentCaptor<Incidencia> incidenciaCaptor = ArgumentCaptor.forClass(Incidencia.class);
        verify(incidenciaRepository).save(incidenciaCaptor.capture());
        
        Incidencia incidenciaGuardada = incidenciaCaptor.getValue();
        assertTrue(incidenciaGuardada.getLugarIncidencia().contains("Error calculando ubicación"));
        
        // La notificación debe salir igual
        verify(notificationService).enviarNotificacion(anyString(), anyString(), anyString());
    }

    // =========================================================================
    // TESTS PARA: listarAlertas (US 33)
    // =========================================================================

    @Test
    void listarAlertas_CaminoFeliz_MapeaCorrectamente() {
        // Arrange
        Persona persona = new Persona();
        persona.setNombre("Juan");
        persona.setApellido("Perez");
        persona.setTelefono("12345678");

        ChoferDetalle chofer = new ChoferDetalle();
        chofer.setIdChofer(1);
        chofer.setPersonaAsociada(persona);
        envioValido.setChofer(chofer);

        Incidencia incidenciaDb = Incidencia.builder()
                .idIncidencia(10)
                .envio(envioValido)
                .tipoIncidencia(TipoIncidencia.CLIMA)
                .estado(EstadoIncidencia.PENDIENTE)
                .fechaReporte(LocalDateTime.now())
                .build();

        when(incidenciaRepository.findAllByOrderByFechaReporteDesc()).thenReturn(List.of(incidenciaDb));

        // Act
        List<AlertaListadoDTO> resultado = incidenciaService.listarAlertas();

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        
        AlertaListadoDTO dto = resultado.get(0);
        assertEquals(10, dto.getId());
        assertEquals("CLIMA", dto.getTipoIncidencia());
        assertEquals("PENDIENTE", dto.getEstado());
        assertEquals("Juan Perez", dto.getChofer().getNombreCompleto());
    }

    // =========================================================================
    // TESTS PARA: resolverIncidencia (US 33)
    // =========================================================================

    @Test
    void resolverIncidencia_CaminoFeliz_CambiaEstadoYGuarda() {
        // Arrange
        Incidencia incidenciaDb = new Incidencia();
        incidenciaDb.setIdIncidencia(1);
        incidenciaDb.setEstado(EstadoIncidencia.PENDIENTE);
        
        when(incidenciaRepository.findById(1)).thenReturn(Optional.of(incidenciaDb));

        ResolverIncidenciaDTO dto = new ResolverIncidenciaDTO();
        dto.setNotasSupervisor("Grua enviada, problema solucionado.");

        // Act
        incidenciaService.resolverIncidencia(1, dto);

        // Assert
        ArgumentCaptor<Incidencia> captor = ArgumentCaptor.forClass(Incidencia.class);
        verify(incidenciaRepository).save(captor.capture());
        
        Incidencia actualizada = captor.getValue();
        assertEquals(EstadoIncidencia.RESUELTA, actualizada.getEstado());
        assertEquals("Grua enviada, problema solucionado.", actualizada.getNotasSupervisor());
        assertNotNull(actualizada.getFechaResolucion());
    }

    @Test
    void resolverIncidencia_CaminoTriste_IncidenciaNoExiste_LanzaException() {
        // Arrange
        when(incidenciaRepository.findById(99)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            incidenciaService.resolverIncidencia(99, new ResolverIncidenciaDTO());
        });

        assertEquals("Incidencia no encontrada", exception.getMessage());
        verify(incidenciaRepository, never()).save(any());
    }
    // =========================================================
    // TICKET #273: Pruebas de resolución de incidencias
    // =========================================================

    @Test
    void resolverIncidencia_CaminoFeliz_CambiaEstadoAResuelta() {
        // Arrange: Creamos una incidencia PENDIENTE
        Incidencia incidenciaPendiente = new Incidencia();
        incidenciaPendiente.setIdIncidencia(1);
        incidenciaPendiente.setEstado(EstadoIncidencia.PENDIENTE);
        
        when(incidenciaRepository.findById(1)).thenReturn(java.util.Optional.of(incidenciaPendiente));

        ResolverIncidenciaDTO dto = new ResolverIncidenciaDTO();
        dto.setNotasSupervisor("Incidencia resuelta correctamente.");

        // Act: Resolvemos
        incidenciaService.resolverIncidencia(1, dto);

        // Assert: Verificamos que el estado cambió a RESUELTA
        ArgumentCaptor<Incidencia> captor = ArgumentCaptor.forClass(Incidencia.class);
        verify(incidenciaRepository).save(captor.capture());
        
        assertEquals(EstadoIncidencia.RESUELTA, captor.getValue().getEstado());
        assertEquals("Incidencia resuelta correctamente.", captor.getValue().getNotasSupervisor());
        assertNotNull(captor.getValue().getFechaResolucion(), "La fecha de resolución no debería ser nula");
    }

    @Test
    void resolverIncidencia_CaminoTriste_IDInexistente_LanzaException() {
        // Arrange: Simulamos que buscamos un ID que no está en la base
        when(incidenciaRepository.findById(999)).thenReturn(java.util.Optional.empty());

        // Act & Assert: Verificamos que el sistema tira error ante un ID inválido
        assertThrows(RuntimeException.class, () -> {
            incidenciaService.resolverIncidencia(999, new ResolverIncidenciaDTO());
        });

        // Verificamos que NUNCA intentó guardar nada
        verify(incidenciaRepository, never()).save(any());
    }
}