package com.logitrack.sistema_logistica.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.sistema_logistica.dto.EnvioOperativoDTO;
import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
import com.logitrack.sistema_logistica.model.*;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoEvento;
import com.logitrack.sistema_logistica.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EnvioServiceTest {

    @Mock private EnvioRepository envioRepository;
    @Mock private EstablecimientoRepository establecimientoRepository;
    @Mock private ChoferDetalleRepository choferDetalleRepository;
    @Mock private CamionRepository camionRepository;
    @Mock private HistorialEstadosRepository historialEstadosRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmpresaClienteRepository empresaClienteRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private GraphHopperService graphHopperService;
    @Mock private RutaEnvioRepository rutaEnvioRepository;

    @InjectMocks
    private EnvioService envioService;

    @BeforeEach
    void setUp() {
        // Inyectamos el valor de @Value("${api.mock.base-url}") manualmente para el test
        ReflectionTestUtils.setField(envioService, "mockBaseUrl", "http://mock-api.com");
    }

    // 1. TEST DEL CAMINO FELIZ: Crear Envío Exitosamente
    @Test
    public void crearNuevoEnvio_DeberiaGuardarEnvioYAuditoria() {
        // Arrange
        EnvioRequestDTO request = new EnvioRequestDTO();
        request.setIdOrigen(1);
        request.setIdDestino(2);
        request.setIdUsuarioCreador(99);
        request.setCpe("12345678");

        Establecimiento origen = new Establecimiento();
        origen.setEmpresa(new EmpresaCliente());
        
        Establecimiento destino = new Establecimiento();
        destino.setEmpresa(new EmpresaCliente());

        Usuario creador = new Usuario();

        // Mockeamos la base de datos
        when(establecimientoRepository.findById(1)).thenReturn(Optional.of(origen));
        when(establecimientoRepository.findById(2)).thenReturn(Optional.of(destino));
        when(usuarioRepository.findById(99)).thenReturn(Optional.of(creador));

        // Mockeamos el servicio externo (ARCA)
        Map<String, String> arcaResponse = Map.of("nroAutorizacion", "AUT-999");
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(arcaResponse, HttpStatus.OK));

        // Simulamos que al guardar devuelve el mismo objeto
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Envio resultado = envioService.crearNuevoEnvio(request);

        // Assert
        assertNotNull(resultado);
        assertEquals(EstadoEnvio.PENDIENTE, resultado.getEstadoActual());
        assertEquals("AUT-999", resultado.getAutorizacionARCA());
        
        // Verificamos que se hayan guardado el envío y el historial
        verify(envioRepository, times(1)).save(any(Envio.class));
        verify(historialEstadosRepository, times(1)).save(any(HistorialEstados.class));
    }

    // 2. TEST DE REGLA DE NEGOCIO: Transición Inválida
    @Test
    public void actualizarEstadoChofer_DeberiaFallarSiSaltaEstados() {
        // Arrange
        String idEnvio = "LT-123";
        String username = "chofer_juan";

        Envio envioMock = mock(Envio.class);
        ChoferDetalle choferMock = mock(ChoferDetalle.class);
        Persona personaMock = mock(Persona.class);
        Usuario usuarioMock = mock(Usuario.class);

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioMock));
        when(envioMock.getChofer()).thenReturn(choferMock);
        when(choferMock.getPersonaAsociada()).thenReturn(personaMock);
        when(personaMock.getIdUsuario()).thenReturn(usuarioMock);
        when(usuarioMock.getUsername()).thenReturn(username);
        
        // Simulo que el envío ya está ENTREGADO
        when(envioMock.getEstadoActual()).thenReturn(EstadoEnvio.ENTREGADO);

        // Act & Assert: Intentamos pasarlo a EN_TRANSITO (Ilegal)
        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> 
            envioService.actualizarEstadoChofer(idEnvio, "EN_TRANSITO", username)
        );

        assertTrue(excepcion.getMessage().contains("Flujo inválido"));
        // Verificamos que NO se guardó nada en BD
        verify(envioRepository, never()).save(any());
    }

    // 3. TEST DE SEGURIDAD / ESTADO: Cancelar Envío
    @Test
    public void cancelarEnvio_DeberiaFallarSiNoEstaPendiente() {
        // Arrange
        String idEnvio = "LT-123";
        String username = "supervisor1";

        Envio envio = new Envio();
        envio.setEstadoActual(EstadoEnvio.EN_TRANSITO); // Ya salió el camión

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));

        // Act & Assert
        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> 
            envioService.cancelarEnvio(idEnvio, username)
        );

        assertTrue(excepcion.getMessage().contains("No se puede cancelar un envío que ya está en ruta"));
    }

    @Test
    public void actualizarEstadoOperativo_DeberiaLanzarExcepcionSiOperadorCambiaPrioridad() {
        // Arrange
        String idEnvio = "LT-777";
        EnvioOperativoDTO dto = new EnvioOperativoDTO();
        dto.setPrioridadIa("ALTA"); // El usuario malicioso intenta subir la prioridad

        Envio envioExistente = Envio.builder().prioridadIa("BAJA").build();
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioExistente));

        // Simulamos un Token de Spring Security (Authentication) con rol de OPERADOR
        org.springframework.security.core.Authentication authMock = mock(org.springframework.security.core.Authentication.class);
        // Le damos un rol que NO es SUPERVISOR
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = 
            java.util.List.of(() -> "ROLE_OPERADOR");
        
        doReturn(authorities).when(authMock).getAuthorities();

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            envioService.actualizarEstadoOperativo(idEnvio, dto, authMock)
        );
        assertTrue(ex.getMessage().contains("solo puede ser modificada por un supervisor"));
    }


   /* @Test
    public void obtenerDetalleConETA_DeberiaCalcularElTiempoCorrectamente() {
        // Arrange
        String idEnvio = "LT-123";
        // Supongamos que el camión salió a las 10:00 AM
        java.time.LocalDateTime fechaSalida = java.time.LocalDateTime.of(2026, 5, 20, 10, 0); 
        
        // Si la distancia es exactamente 65.0 km, a 65 km/h debería tardar exactamente 60 minutos.
        Envio envio = Envio.builder()
                .idEnvio(idEnvio)
                .distanciaKm(65.0)
                .fechaSalida(fechaSalida)
                .build();

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));

        // Act
        com.logitrack.sistema_logistica.dto.EnvioDetalleResponseDTO detalle = 
            envioService.obtenerDetalleConETA(idEnvio);

        // Assert
        assertNotNull(detalle);
        // Validamos que el ETA calculado sea exactamente a las 11:00 AM (10:00 + 60 mins)
        assertEquals(java.time.LocalDateTime.of(2026, 5, 20, 11, 0), detalle.getFechaEstimadaLlegada());
    }*/

/*
@Test
    public void obtenerHistorialPorEnvio_DeberiaRetornarListaDeDTOs() {
        // Arrange
        String idEnvio = "LT-123";
        
        // Simulamos que el envío sí existe
        when(envioRepository.existsById(idEnvio)).thenReturn(true);
        
        // Creamos un historial falso en la BD
        HistorialEstados historialFalso = HistorialEstados.builder()
                .idHistorial(1)
                .estadoNuevo(EstadoEnvio.PENDIENTE)
                .tipoEvento(TipoEvento.CREACION)
                .usuario(new Usuario()) // Evitar NullPointerException en fromEntity si lo requiere
                .build();
                
        when(historialEstadosRepository.buscarHistorialPorEnvio(idEnvio))
                .thenReturn(java.util.List.of(historialFalso));

        // Act
        java.util.List<com.logitrack.sistema_logistica.dto.HistorialResponseDTO> resultado = 
                envioService.obtenerHistorialPorEnvio(idEnvio);

        // Assert
        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        // Verificamos que el mapeo interno conservó el estado
        assertEquals(EstadoEnvio.PENDIENTE.name(), resultado.get(0).getEstadoNuevo());
    }
*/
@Test
    public void obtenerGeometriaRuta_DeberiaLanzarExcepcionSiNoHayRuta() {
        // Arrange
        String idEnvio = "LT-999";
        
        // Creamos un envío, pero NO le asignamos ningún objeto RutaEnvio
        Envio envioSinRuta = Envio.builder().idEnvio(idEnvio).rutaEnvio(null).build();
        
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioSinRuta));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            envioService.obtenerGeometriaRuta(idEnvio)
        );
        
        assertEquals("El envío no tiene una ruta generada aún.", ex.getMessage());
    }
/*
    @Test
    public void actualizarEstadoOperativo_NoDeberiaCrearHistorialSiEstadoEsElMismo() {
        // Arrange
        String idEnvio = "LT-456";
        
        // El envío en BD está PENDIENTE y con Prioridad ALTA
        Envio envioExistente = Envio.builder()
                .estadoActual(EstadoEnvio.PENDIENTE)
                .prioridadIa("ALTA")
                .build();
                
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioExistente));
        
        // El DTO del request manda exactamente lo mismo
        EnvioOperativoDTO dto = new EnvioOperativoDTO();
        dto.setEstado(EstadoEnvio.PENDIENTE);
        dto.setPrioridadIa("ALTA");
        
        // Necesitamos mockear el Auth aunque no haga nada, para evitar NullPointer
        org.springframework.security.core.Authentication authMock = mock(org.springframework.security.core.Authentication.class);
        
        when(envioRepository.save(any(Envio.class))).thenReturn(envioExistente);

        // Act
        Envio resultado = envioService.actualizarEstadoOperativo(idEnvio, dto, authMock);

        // Assert
        assertEquals(EstadoEnvio.PENDIENTE, resultado.getEstadoActual());
        
        // LA CLAVE: Verificamos que el repositorio de historial NUNCA fue llamado
        verify(historialEstadosRepository, never()).save(any(HistorialEstados.class));
    }*/

}

