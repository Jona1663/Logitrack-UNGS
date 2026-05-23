package com.logitrack.sistema_logistica.service;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.logitrack.sistema_logistica.dto.EnvioOperativoDTO;
import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.EmpresaCliente;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.model.HistorialEstados;
import com.logitrack.sistema_logistica.model.Persona;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoEvento;
import com.logitrack.sistema_logistica.repository.CamionRepository;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.EmpresaClienteRepository;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.EstablecimientoRepository;
import com.logitrack.sistema_logistica.repository.HistorialEstadosRepository;
import com.logitrack.sistema_logistica.repository.RutaEnvioRepository;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class EnvioServiceTest {

    @Mock private EnvioRepository envioRepository;
    @Mock private EstablecimientoRepository establecimientoRepository;
    @Mock private ChoferDetalleRepository choferDetalleRepository;
    @Mock private CamionRepository camionRepository;
    @Mock private HistorialEstadosRepository historialEstadosRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmpresaClienteRepository empresaClienteRepository;
    @Mock private RutaEnvioRepository rutaEnvioRepository;
    @Mock private HistorialEstadosRepository historialRepository;
    
    // NUEVOS SERVICIOS (Reemplazan a GraphHopper y RestTemplate)
    @Mock private ValidacionExternaService validacionExternaService;
    @Mock private TrackingGeospatialService trackingService;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private EnvioService envioService;

    // YA NO HAY setUp() con mockBaseUrl porque eso ahora vive en ValidacionExternaService

    @Test
    public void crearNuevoEnvio_DeberiaGuardarEnvioYAuditoria() {
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

        when(establecimientoRepository.findById(1)).thenReturn(Optional.of(origen));
        when(establecimientoRepository.findById(2)).thenReturn(Optional.of(destino));
        when(usuarioRepository.findById(99)).thenReturn(Optional.of(creador));

        when(validacionExternaService.getNroAutorizacionArca(anyString())).thenReturn("AUT-999");
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);

        Envio resultado = envioService.crearNuevoEnvio(request);

        assertNotNull(resultado);
        assertEquals(EstadoEnvio.PENDIENTE, resultado.getEstadoActual());
        assertEquals("AUT-999", resultado.getAutorizacionARCA());
        
        verify(envioRepository, times(1)).save(any(Envio.class));
        verify(auditoriaService, times(1)).registrarEvento(any(), any(), any(), any(), any());
    }

    @Test
    public void actualizarEstadoChofer_DeberiaFallarSiSaltaEstados() {
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
        
        when(envioMock.getEstadoActual()).thenReturn(EstadoEnvio.ENTREGADO);

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> 
            envioService.actualizarEstadoChofer(idEnvio, "EN_TRANSITO", username)
        );

        assertTrue(excepcion.getMessage().contains("Flujo inválido"));
        verify(envioRepository, never()).save(any());
    }

    @Test
    public void cancelarEnvio_DeberiaFallarSiNoEstaPendiente() {
        String idEnvio = "LT-123";
        String username = "supervisor1";

        Envio envio = new Envio();
        envio.setEstadoActual(EstadoEnvio.EN_TRANSITO); 

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> 
            envioService.cancelarEnvio(idEnvio, username)
        );

        assertTrue(excepcion.getMessage().contains("No se puede cancelar un envío que ya está en ruta"));
    }

    @Test
    public void actualizarEstadoOperativo_DeberiaLanzarExcepcionSiOperadorCambiaPrioridad() {
        String idEnvio = "LT-777";
        EnvioOperativoDTO dto = new EnvioOperativoDTO();
        dto.setPrioridadIa("ALTA");

        Envio envioExistente = Envio.builder().prioridadIa("BAJA").build();
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioExistente));

        org.springframework.security.core.Authentication authMock = mock(org.springframework.security.core.Authentication.class);
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = 
            java.util.List.of(() -> "ROLE_OPERADOR");
        
        doReturn(authorities).when(authMock).getAuthorities();

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            envioService.actualizarEstadoOperativo(idEnvio, dto, authMock)
        );
        assertTrue(ex.getMessage().contains("solo puede ser modificada por un supervisor"));
    }

    @Test
    public void obtenerDetalleConETA_DeberiaCalcularElTiempoCorrectamente() {
        String idEnvio = "LT-123";
        java.time.LocalDateTime fechaSalida = java.time.LocalDateTime.of(2026, 5, 20, 10, 0); 
        Establecimiento origen = new Establecimiento();
        origen.setNombreLugar("Origen Test");
        Establecimiento destino = new Establecimiento();
        destino.setNombreLugar("destino test");
        Persona persona = new Persona();
        persona.setIdPersona(1);
        ChoferDetalle chofer = new ChoferDetalle();
        chofer.setIdChofer(1);
        chofer.setPersonaAsociada(persona);
        Camion camion = new Camion();
        camion.setPatente("lic-123");
        
        Envio envio = Envio.builder()
                .idEnvio(idEnvio)
                .distanciaKm(65.0)
                .fechaSalida(fechaSalida)
                .origen(origen)
                .destino(destino)
                .chofer(chofer)
                .camion(camion)
                .build();

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));

        java.time.LocalDateTime etaEsperado = java.time.LocalDateTime.of(2026, 5, 20, 11, 0);
        
        // MOCKEAMOS EL NUEVO SERVICIO ESPACIAL
        when(trackingService.calcularETA(any(), any())).thenReturn(etaEsperado);

        com.logitrack.sistema_logistica.dto.EnvioDetalleResponseDTO detalle = 
            envioService.obtenerDetalleConETA(idEnvio);

        assertNotNull(detalle);
        assertEquals(etaEsperado, detalle.getFechaEstimadaLlegada());
    }

    @Test
    public void obtenerHistorialPorEnvio_DeberiaRetornarListaDeDTOs() {
        String idEnvio = "LT-123";
        when(envioRepository.existsById(idEnvio)).thenReturn(true);
        
        HistorialResponseDTO dtoFalso = new HistorialResponseDTO();
        dtoFalso.setEstadoNuevo(EstadoEnvio.PENDIENTE.name());
        
        // 3. MOCKEAMOS EL AUDITORIA SERVICE (Aquí está la clave)
        when(auditoriaService.obtenerHistorialPorEnvio(idEnvio))
                .thenReturn(java.util.List.of(dtoFalso));

        // Act
        java.util.List<com.logitrack.sistema_logistica.dto.HistorialResponseDTO> resultado = 
                envioService.obtenerHistorialPorEnvio(idEnvio);

        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        assertEquals(EstadoEnvio.PENDIENTE.name(), resultado.get(0).getEstadoNuevo());
    }

    @Test
    public void obtenerGeometriaRuta_DeberiaLanzarExcepcionSiNoHayRuta() {
        String idEnvio = "LT-999";
        Envio envioSinRuta = Envio.builder().idEnvio(idEnvio).rutaEnvio(null).build();
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioSinRuta));

        // MOCKEAMOS EL NUEVO SERVICIO PARA QUE TIRE EL ERROR
        when(trackingService.extraerGeometriaRuta(null))
            .thenThrow(new RuntimeException("El envío no tiene una ruta generada aún."));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            envioService.obtenerGeometriaRuta(idEnvio)
        );
        
        assertEquals("El envío no tiene una ruta generada aún.", ex.getMessage());
    }

    @Test
    public void actualizarEstadoYPrioridad_DeberiaActualizarYGuardarHistorial() {
        String idEnvio = "LT-123";
        Establecimiento origen = new Establecimiento();
        origen.setLatitud(-34.0);
        origen.setLongitud(-58.0);
        
        Establecimiento destino = new Establecimiento();
        destino.setLatitud(-35.0);
        destino.setLongitud(-59.0);

        Envio envio = Envio.builder()
                .idEnvio(idEnvio)
                .estadoActual(EstadoEnvio.PENDIENTE)
                .prioridadIa("BAJA")
                .origen(origen)   
                .destino(destino) 
                .build();
        Usuario user = new Usuario();

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);
        
        // BORRAMOS EL MOCK DE GRAPHHOPPER PORQUE YA NO SE USA AQUÍ

        // Act (Se llama 1 sola vez)
        envioService.actualizarEstadoYPrioridad(idEnvio, "EN_TRANSITO", "ALTA", user, TipoEvento.CAMBIO_ESTADO);

        // Assert
        assertEquals(EstadoEnvio.EN_TRANSITO, envio.getEstadoActual());
        assertEquals("ALTA", envio.getPrioridadIa());
        verify(auditoriaService, times(1)).registrarEvento(any(), any(), any(), any(), any());
    }

    @Test
    public void editarEnvio_DeberiaActualizarDatosYGuardarHistorial() {
        String idEnvio = "LT-123";
        EnvioRequestDTO dto = new EnvioRequestDTO();
        dto.setIdChofer(1);
        dto.setPatenteCamion("ABC-123");
        
        Envio envio = Envio.builder().estadoActual(EstadoEnvio.PENDIENTE).build();
        
        ChoferDetalle chofer = new ChoferDetalle();
        chofer.setVtoLicencia(java.time.LocalDate.now().plusDays(10));
        chofer.setVtoLinti(java.time.LocalDate.now().plusDays(10));
        
        Camion camion = new Camion();
        camion.setVtoSenasa(java.time.LocalDate.now().plusDays(10));

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));
        when(choferDetalleRepository.findById(1)).thenReturn(Optional.of(chofer)); 
        when(camionRepository.findById("ABC-123")).thenReturn(Optional.of(camion)); 
        when(usuarioRepository.findByUsername("user")).thenReturn(Optional.of(new Usuario()));
        when(envioRepository.save(any(Envio.class))).thenReturn(envio);

        envioService.editarEnvio(idEnvio, dto, "user");

        verify(envioRepository, times(1)).save(any(Envio.class));
        verify(auditoriaService, times(1)).registrarEvento(any(), any(), any(), any(), any());
    }

    @Test
    public void intentarAccion_EnViajeFinalizado_DeberiaLanzarExcepcion() {
        String idEnvio = "LT-FINALIZADO-99";
        Envio envioFinalizado = new Envio();
        envioFinalizado.setIdEnvio(idEnvio);
        envioFinalizado.setEstadoActual(EstadoEnvio.ENTREGADO); 
        
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioFinalizado));
        EnvioRequestDTO dtoFalso = new EnvioRequestDTO(); 

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            envioService.editarEnvio(idEnvio, dtoFalso, "tester_qa");
        });

        assertTrue(excepcion.getMessage().contains("No se pueden modificar los datos"));
        verify(envioRepository, never()).save(any());
    }
}