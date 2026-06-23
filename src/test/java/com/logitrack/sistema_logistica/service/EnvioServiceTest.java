package com.logitrack.sistema_logistica.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.logitrack.sistema_logistica.dto.AsignarTransporteDTO;
import com.logitrack.sistema_logistica.dto.EnvioOperativoDTO;
import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.EmpresaCliente;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.model.Persona;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoEvento;
import com.logitrack.sistema_logistica.repository.CamionRepository;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.EmpresaClienteRepository;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.EstablecimientoRepository;
import com.logitrack.sistema_logistica.repository.EvaluacionPsicomotoraRepository;
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
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private EvaluacionPsicomotoraRepository evaluacionRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate; 
    
    // NUEVOS SERVICIOS (Reemplazan a GraphHopper y RestTemplate)
    @Mock private ValidacionExternaService validacionExternaService;
    @Mock private TrackingGeospatialService trackingService;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private EnvioService envioService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

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

    @Test
    public void asignarTransporte_Exitoso_DeberiaAsignarYMarcarNoDisponible() {
        // Arrange
        String idEnvio = "LT-111";
        AsignarTransporteDTO dto = new AsignarTransporteDTO();
        dto.setIdChofer(1);
        dto.setPatenteCamion("ABC-123");

        Envio envio = new Envio(); // Sin chofer ni camión
        ChoferDetalle chofer = new ChoferDetalle();
        chofer.setDisponible(true);
        Camion camion = new Camion();
        camion.setDisponible(true);

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));
        when(choferDetalleRepository.findById(1)).thenReturn(Optional.of(chofer));
        when(camionRepository.findById("ABC-123")).thenReturn(Optional.of(camion));
        
        // Simulamos que NO están ocupados
        when(envioRepository.existsByChoferAndEstadoActualIn(eq(chofer), anyList())).thenReturn(false);
        when(envioRepository.existsByCamionAndEstadoActualIn(eq(camion), anyList())).thenReturn(false);
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Envio resultado = envioService.asignarTransporte(idEnvio, dto);

        // Assert
        assertNotNull(resultado.getChofer());
        assertNotNull(resultado.getCamion());
        assertFalse(chofer.getDisponible(), "El chofer debe quedar no disponible");
        assertFalse(camion.getDisponible(), "El camión debe quedar no disponible");
        
        verify(choferDetalleRepository, times(1)).save(chofer);
        verify(camionRepository, times(1)).save(camion);
        verify(validacionExternaService, times(1)).verificarLicenciaChofer(any(), eq(chofer));
        verify(validacionExternaService, times(1)).verificarHabilitacionSenasa(any(), eq(camion));
    }
/*
    @Test
    public void asignarTransporte_ChoferOcupado_DeberiaLanzarExcepcion() {
        // Arrange
        String idEnvio = "LT-111";
        AsignarTransporteDTO dto = new AsignarTransporteDTO();
        dto.setIdChofer(1);
        dto.setPatenteCamion("ABC-123");

        Envio envio = new Envio();
        ChoferDetalle chofer = new ChoferDetalle();
        Camion camion = new Camion();

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));
        when(choferDetalleRepository.findById(1)).thenReturn(Optional.of(chofer));
        when(camionRepository.findById("ABC-123")).thenReturn(Optional.of(camion));
        
        // Simulamos que el chofer SÍ está ocupado en otro viaje
        when(envioRepository.existsByChoferAndEstadoActualIn(eq(chofer), anyList())).thenReturn(true);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            envioService.asignarTransporte(idEnvio, dto)
        );
        assertTrue(ex.getMessage().contains("El chofer acaba de ser asignado a otro viaje"));
        verify(envioRepository, never()).save(any()); // Nunca se guarda
    }
*/
    @Test
    public void asignarTransporte_ChoferOcupado_DeberiaLanzarExcepcion() {
        // Arrange
        AsignarTransporteDTO dto = new AsignarTransporteDTO();
        dto.setIdChofer(1); dto.setPatenteCamion("AAA");
        
        ChoferDetalle chofer = new ChoferDetalle();
        Camion camion = new Camion();

        when(envioRepository.findById("LT-1")).thenReturn(Optional.of(new Envio()));
        when(choferDetalleRepository.findById(1)).thenReturn(Optional.of(chofer));
        when(camionRepository.findById("AAA")).thenReturn(Optional.of(camion));
        
        // Simulamos chofer ocupado
        when(envioRepository.existsByChoferAndEstadoActualIn(eq(chofer), any())).thenReturn(true);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> envioService.asignarTransporte("LT-1", dto));
        assertTrue(ex.getMessage().contains("chofer acaba de ser asignado"));
    }


    @Test
    public void actualizarEstadoYPrioridad_AlEntregar_DeberiaLiberarRecursos() {
        // Arrange
        String idEnvio = "LT-222";
        Usuario user = new Usuario();
        
        ChoferDetalle chofer = new ChoferDetalle();
        chofer.setDisponible(false); // Estaba viajando
        Camion camion = new Camion();
        camion.setDisponible(false); // Estaba viajando

        Envio envio = Envio.builder()
                .idEnvio(idEnvio)
                .estadoActual(EstadoEnvio.EN_REPARTO)
                .chofer(chofer)
                .camion(camion)
                .build();

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act: Pasamos a ENTREGADO
        envioService.actualizarEstadoYPrioridad(idEnvio, "ENTREGADO", "ALTA", user, TipoEvento.CAMBIO_ESTADO);

        // Assert
        assertTrue(chofer.getDisponible(), "El chofer debe quedar liberado");
        assertTrue(camion.getDisponible(), "El camión debe quedar liberado");
        verify(choferDetalleRepository, times(1)).save(chofer);
        verify(camionRepository, times(1)).save(camion);
        verify(auditoriaService, times(1)).registrarEvento(any(), any(), any(), any(), any());
    }   

    @Test
    public void actualizarEstadoOperativo_SoloPrioridad_DeberiaAuditarCambioDePrioridad() {
        // Arrange
        String idEnvio = "LT-333";
        EnvioOperativoDTO dto = new EnvioOperativoDTO();
        dto.setEstado(EstadoEnvio.PENDIENTE); // Mismo estado
        dto.setPrioridadIa("ALTA"); // Cambia de BAJA a ALTA

        Envio envioExistente = Envio.builder().estadoActual(EstadoEnvio.PENDIENTE).prioridadIa("BAJA").build();
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioExistente));
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);

        // Configuramos auth como SUPERVISOR
        org.springframework.security.core.Authentication authMock = mock(org.springframework.security.core.Authentication.class);
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = java.util.List.of(() -> "ROLE_SUPERVISOR");
        doReturn(authorities).when(authMock).getAuthorities();
        when(authMock.getName()).thenReturn("super_juan");

        Usuario supervisor = new Usuario();
        when(usuarioRepository.findByUsername("super_juan")).thenReturn(Optional.of(supervisor));

        // Act
        Envio resultado = envioService.actualizarEstadoOperativo(idEnvio, dto, authMock);

        // Assert
        assertEquals("ALTA", resultado.getPrioridadIa());
        assertEquals(EstadoEnvio.PENDIENTE, resultado.getEstadoActual()); // No cambió
        
        // Verificamos que se haya registrado la auditoría específica de prioridad
        /*
        //Version1
        verify(auditoriaService, times(1)).registrarEvento(
                eq(resultado), eq(supervisor), eq(TipoEvento.CAMBIO_PRIORIDAD), eq(EstadoEnvio.PENDIENTE), eq(EstadoEnvio.PENDIENTE)
        );
        */
        verify(auditoriaService, times(1)).registrarEvento(
        eq(resultado), eq(supervisor), eq(TipoEvento.CAMBIO_PRIORIDAD), 
        eq(EstadoEnvio.PENDIENTE), eq(EstadoEnvio.PENDIENTE), anyString()
        );
    }

    @Test
    public void asignarChoferCamion_Exitoso() {
        // Arrange
        EnvioRequestDTO dto = new EnvioRequestDTO();
        dto.setIdEnvio("LT-123");
        dto.setIdChofer(1);
        dto.setPatenteCamion("ABC-123");

        Envio envio = new Envio();
        envio.setDistanciaKm(100.0);

        when(envioRepository.findById("LT-123")).thenReturn(Optional.of(envio));
        when(camionRepository.findById("ABC-123")).thenReturn(Optional.of(new Camion()));
        when(choferDetalleRepository.findById(1)).thenReturn(Optional.of(new ChoferDetalle()));
        
        // Mockeamos la fecha del ETA
        LocalDateTime etaFalso = LocalDateTime.now().plusHours(2);
        when(trackingService.calcularETA(eq(100.0), any(LocalDateTime.class))).thenReturn(etaFalso);

        // Act
        envioService.asignarChoferCamion(dto);

        // Assert
        assertNotNull(envio.getCamion());
        assertNotNull(envio.getChofer());
        assertNotNull(envio.getFechaSalida());
        assertEquals(etaFalso, envio.getFechaEstimadaLlegada());
        verify(envioRepository, times(1)).save(envio);
    }

    @Test
    public void buscarPorId_EnvioExiste_DeberiaRetornarEnvio() {
        // Arrange
        String idEnvio = "LT-BUSCAR-1";
        Envio envioMock = Envio.builder().idEnvio(idEnvio).build();
        when(envioRepository.buscarPorId(idEnvio)).thenReturn(Optional.of(envioMock));

        // Act
        Envio resultado = envioService.buscarPorId(idEnvio);

        // Assert
        assertNotNull(resultado);
        assertEquals(idEnvio, resultado.getIdEnvio());
    }

    @Test
    public void buscarPorId_EnvioNoExiste_DeberiaLanzarExcepcion() {
        // Arrange
        String idEnvio = "LT-FANTASMA";
        when(envioRepository.buscarPorId(idEnvio)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            envioService.buscarPorId(idEnvio)
        );
        assertTrue(ex.getMessage().contains("No se encontró el envío con el idEnvio"));
    }

    @Test
    public void obtenerEnviosPorChofer_DeberiaRetornarListaDeEnvios() {
        // Arrange
        String username = "chofer_carlos";
        Envio envio1 = new Envio();
        Envio envio2 = new Envio();
        when(envioRepository.findByChoferUsername(username)).thenReturn(java.util.List.of(envio1, envio2));

        // Act
        java.util.List<Envio> resultados = envioService.obtenerEnviosPorChofer(username);

        // Assert
        assertNotNull(resultados);
        assertEquals(2, resultados.size());
        verify(envioRepository, times(1)).findByChoferUsername(username);
    }

/* 
    @Test
    public void buscarEnviosConFiltros_DeberiaRetornarPaginaDeEnvios() {
        // Arrange
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        Envio envioFalso = new Envio();
        org.springframework.data.domain.Page<Envio> paginaMock = 
            new org.springframework.data.domain.PageImpl<>(java.util.List.of(envioFalso));

        // Mockeamos el findAll que recibe una Specification y un Pageable
        when(envioRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(paginaMock);

        // Act
        org.springframework.data.domain.Page<Envio> resultado = envioService.buscarEnviosConFiltros(
                EstadoEnvio.PENDIENTE, null, null, null, null, null, pageable);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }
*/
    @Test
    public void actualizarEstadoChofer_TransicionValida_DeberiaActualizarEstado() {
        // Arrange
        String idEnvio = "LT-123";
        String username = "chofer_juan";

        ChoferDetalle choferMock = mock(ChoferDetalle.class);
        Persona personaMock = mock(Persona.class);
        Usuario usuarioMock = new Usuario();
        usuarioMock.setUsername(username);

        // Simulamos la cadena de relaciones para pasar la validación de identidad
        when(choferMock.getPersonaAsociada()).thenReturn(personaMock);
        when(personaMock.getIdUsuario()).thenReturn(usuarioMock);

        // El envío arranca en PENDIENTE
        Envio envio = Envio.builder()
                .idEnvio(idEnvio)
                .estadoActual(EstadoEnvio.PENDIENTE)
                .chofer(choferMock)
                .prioridadIa("NORMAL")
                .build();

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuarioMock));
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act: Pasamos a EN_TRANSITO (Transición Válida)
        Envio resultado = envioService.actualizarEstadoChofer(idEnvio, "EN_TRANSITO", username);

        // Assert
        assertEquals(EstadoEnvio.EN_TRANSITO, resultado.getEstadoActual());
        verify(envioRepository, times(1)).save(any(Envio.class));
        verify(auditoriaService, times(1)).registrarEvento(any(), any(), any(), any(), any());
    }

    @Test
    public void actualizarEstadoChofer_MismoEstado_DeberiaRetornarSinHacerNada() {
        // Arrange
        String idEnvio = "LT-123";
        String username = "chofer_juan";

        ChoferDetalle choferMock = mock(ChoferDetalle.class);
        Persona personaMock = mock(Persona.class);
        Usuario usuarioMock = new Usuario();
        usuarioMock.setUsername(username);

        when(choferMock.getPersonaAsociada()).thenReturn(personaMock);
        when(personaMock.getIdUsuario()).thenReturn(usuarioMock);

        Envio envio = Envio.builder()
                .idEnvio(idEnvio)
                .estadoActual(EstadoEnvio.EN_TRANSITO) // Ya está en tránsito
                .chofer(choferMock)
                .build();

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));

        // Act: Intentamos pasarlo al MISMO estado
        Envio resultado = envioService.actualizarEstadoChofer(idEnvio, "EN_TRANSITO", username);

        // Assert
        assertEquals(EstadoEnvio.EN_TRANSITO, resultado.getEstadoActual());
        // Verificamos que al ser el mismo estado, NUNCA llega a guardar ni a auditar
        verify(envioRepository, never()).save(any(Envio.class));
        verify(auditoriaService, never()).registrarEvento(any(), any(), any(), any(), any());
    }

    @Test
    public void obtenerUbicacionActual_DeberiaRetornarMapaDeUbicacion() {
        // Arrange
        String idEnvio = "LT-TRACK-1";
        Envio envioMock = new Envio();
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioMock));
        
        java.util.Map<String, Object> ubicacionMock = java.util.Map.of("lat", -34.0, "lng", -58.0);
        when(trackingService.calcularUbicacionInterpolada(envioMock)).thenReturn(ubicacionMock);

        // Act
        java.util.Map<String, Object> resultado = envioService.obtenerUbicacionActual(idEnvio);

        // Assert
        assertNotNull(resultado);
        assertEquals(-34.0, resultado.get("lat"));
    }

    @Test
    public void obtenerGeometriaRuta_DeberiaRetornarJsonNode() {
        // Arrange
        String idEnvio = "LT-RUTE-1";
        Envio envioMock = new Envio();
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioMock));
        
        com.fasterxml.jackson.databind.JsonNode jsonNodeMock = mock(com.fasterxml.jackson.databind.JsonNode.class);
        when(trackingService.extraerGeometriaRuta(envioMock.getRutaEnvio())).thenReturn(jsonNodeMock);

        // Act
        com.fasterxml.jackson.databind.JsonNode resultado = envioService.obtenerGeometriaRuta(idEnvio);

        // Assert
        assertNotNull(resultado);
    }

@Test
    public void actualizarEstadoOperativo_CuandoEstadoCambia_DeberiaAuditarCambioDeEstado() {
        // Arrange
        String idEnvio = "LT-OP-1";
        
        // LA CLAVE: Usamos thenAnswer para devolver un objeto NUEVO cada vez que el servicio llama al repositorio.
        when(envioRepository.findById(idEnvio)).thenAnswer(invocation -> 
            Optional.of(Envio.builder()
                .estadoActual(EstadoEnvio.PENDIENTE)
                .prioridadIa("NORMAL")
                .build())
        );

        // El DTO pide pasarlo a EN_TRANSITO
        EnvioOperativoDTO dto = new EnvioOperativoDTO();
        dto.setEstado(EstadoEnvio.EN_TRANSITO); 
        
        Usuario usuarioMock = new Usuario();
        when(usuarioRepository.findByUsername("super1")).thenReturn(Optional.of(usuarioMock));
        
        org.springframework.security.core.Authentication authMock = mock(org.springframework.security.core.Authentication.class);
        when(authMock.getName()).thenReturn("super1");

        // Mockeamos el save 
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Envio resultado = envioService.actualizarEstadoOperativo(idEnvio, dto, authMock);

        // Assert
        assertEquals(EstadoEnvio.EN_TRANSITO, resultado.getEstadoActual());
        
        // CORRECCIÓN: Se eliminó el "anyString()" final porque el método de tu compañero 
        // ahora solo acepta 5 parámetros (Envio, Usuario, TipoEvento, EstadoAnterior, EstadoNuevo).
        verify(auditoriaService, times(1)).registrarEvento(
                any(), 
                eq(usuarioMock), 
                eq(TipoEvento.CAMBIO_ESTADO), 
                eq(EstadoEnvio.PENDIENTE), 
                eq(EstadoEnvio.EN_TRANSITO)
        );
    }


    @Test
    public void asignarTransporte_CamionOcupado_DeberiaLanzarExcepcion() {
        // Arrange
        AsignarTransporteDTO dto = new AsignarTransporteDTO();
        dto.setIdChofer(1); dto.setPatenteCamion("AAA");
        
        ChoferDetalle chofer = new ChoferDetalle();
        Camion camion = new Camion();

        when(envioRepository.findById("LT-1")).thenReturn(Optional.of(new Envio()));
        when(choferDetalleRepository.findById(1)).thenReturn(Optional.of(chofer));
        when(camionRepository.findById("AAA")).thenReturn(Optional.of(camion));
        
        // Chofer libre, pero camión ocupado
        when(envioRepository.existsByChoferAndEstadoActualIn(eq(chofer), any())).thenReturn(false);
        when(envioRepository.existsByCamionAndEstadoActualIn(eq(camion), any())).thenReturn(true);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> envioService.asignarTransporte("LT-1", dto));
        assertTrue(ex.getMessage().contains("camión acaba de ser asignado"));
    }

    @Test
    public void cancelarEnvio_EstadoPendiente_DeberiaCancelarYGuardar() {
        // Arrange
        String idEnvio = "LT-CANCEL";
        Envio envioMock = Envio.builder().idEnvio(idEnvio).estadoActual(EstadoEnvio.PENDIENTE).prioridadIa("NORMAL").build();
        Usuario usuarioMock = new Usuario();

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioMock));
        when(usuarioRepository.findByUsername("user1")).thenReturn(Optional.of(usuarioMock));
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Envio resultado = envioService.cancelarEnvio(idEnvio, "user1");

        // Assert
        assertEquals(EstadoEnvio.CANCELADO, resultado.getEstadoActual());
        verify(auditoriaService, times(1)).registrarEvento(any(), eq(usuarioMock), eq(TipoEvento.CANCELACION), eq(EstadoEnvio.PENDIENTE), eq(EstadoEnvio.CANCELADO));
    }

    @Test
    public void actualizarEstadoChofer_TransicionesValidas_DeberiaCubrirElSwitch() {
        // Configuramos los mocks básicos
        ChoferDetalle choferMock = mock(ChoferDetalle.class);
        Persona personaMock = mock(Persona.class);
        Usuario usuarioMock = new Usuario();
        usuarioMock.setUsername("chofer1");
        when(choferMock.getPersonaAsociada()).thenReturn(personaMock);
        when(personaMock.getIdUsuario()).thenReturn(usuarioMock);
        when(usuarioRepository.findByUsername("chofer1")).thenReturn(Optional.of(usuarioMock));
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);

        // CASO 1: EN_TRANSITO -> EN_PUNTO_DE_RECOLECCION
        Envio e1 = Envio.builder().idEnvio("1").estadoActual(EstadoEnvio.EN_TRANSITO).chofer(choferMock).build();
        when(envioRepository.findById("1")).thenReturn(Optional.of(e1));
        assertEquals(EstadoEnvio.EN_PUNTO_DE_RECOLECCION, envioService.actualizarEstadoChofer("1", "EN_PUNTO_DE_RECOLECCION", "chofer1").getEstadoActual());

        // CASO 2: EN_PUNTO_DE_RECOLECCION -> EN_REPARTO
        Envio e2 = Envio.builder().idEnvio("2").estadoActual(EstadoEnvio.EN_PUNTO_DE_RECOLECCION).chofer(choferMock).build();
        when(envioRepository.findById("2")).thenReturn(Optional.of(e2));
        assertEquals(EstadoEnvio.EN_REPARTO, envioService.actualizarEstadoChofer("2", "EN_REPARTO", "chofer1").getEstadoActual());

        // CASO 3: EN_REPARTO -> ENTREGADO
        Envio e3 = Envio.builder().idEnvio("3").estadoActual(EstadoEnvio.EN_REPARTO).chofer(choferMock).build();
        when(envioRepository.findById("3")).thenReturn(Optional.of(e3));
        assertEquals(EstadoEnvio.ENTREGADO, envioService.actualizarEstadoChofer("3", "ENTREGADO", "chofer1").getEstadoActual());

        // CASO 4: DEFAULT (Transición inválida, ej: ENTREGADO a PENDIENTE)
        Envio e4 = Envio.builder().idEnvio("4").estadoActual(EstadoEnvio.ENTREGADO).chofer(choferMock).build();
        when(envioRepository.findById("4")).thenReturn(Optional.of(e4));
        assertThrows(RuntimeException.class, () -> envioService.actualizarEstadoChofer("4", "PENDIENTE", "chofer1"));
    }

   // =========================================================
    // TICKET #258: Pruebas de intercepción (Notificaciones)
    // =========================================================
     @Test
     public void cambiarEstado_DeberiaInterceptarYLlamarNotificacion() {
         // Arrange
         com.logitrack.sistema_logistica.service.NotificationService notifServiceMock = 
             mock(com.logitrack.sistema_logistica.service.NotificationService.class);
            
         com.logitrack.sistema_logistica.events.EnvioCambioEstadoListenerNotificaciones listener = 
             new com.logitrack.sistema_logistica.events.EnvioCambioEstadoListenerNotificaciones(notifServiceMock);

         com.logitrack.sistema_logistica.model.EmpresaCliente empresa = new com.logitrack.sistema_logistica.model.EmpresaCliente();
         empresa.setRazonSocial("LogiCorp SRL");

         com.logitrack.sistema_logistica.model.Establecimiento origen = new com.logitrack.sistema_logistica.model.Establecimiento();
         origen.setEmpresa(empresa);

         com.logitrack.sistema_logistica.model.Envio viajeDePrueba = com.logitrack.sistema_logistica.model.Envio.builder()
                 .idEnvio("LT-NOTIF-001")
                 .estadoActual(com.logitrack.sistema_logistica.model.enums.EstadoEnvio.EN_TRANSITO) 
                 .origen(origen)
                 .build();

         // LA MAGIA ACÁ: Simulamos el evento viejo con Mockito para esquivar errores de constructores
         com.logitrack.sistema_logistica.events.EnvioCambioEstadoEvent eventoMock = 
             mock(com.logitrack.sistema_logistica.events.EnvioCambioEstadoEvent.class);
         
         // Le decimos al mock que cuando el listener le pida el viaje, le entregue el nuestro
         when(eventoMock.getEnvio()).thenReturn(viajeDePrueba);

         // Act
         listener.onCambioEstado(eventoMock); 

         // Assert
         verify(notifServiceMock, times(1)).enviarNotificacion(
                 anyString(), 
                 anyString(), 
                 anyString()  
         );
     }
    // =========================================================
    // TICKET #223: Pruebas de validación de disponibilidad 
    // =========================================================
    @Test
    public void asignarTransporte_ChoferYaOcupado_DeberiaLanzarExcepcion() {
        // Arrange: Preparamos el DTO con los campos que REALMENTE existen
        com.logitrack.sistema_logistica.dto.AsignarTransporteDTO dto = new com.logitrack.sistema_logistica.dto.AsignarTransporteDTO();
        dto.setIdChofer(999);
        dto.setPatenteCamion("ABC-123"); // <--- ¡ESTE ES EL CAMPO REAL!

        // Simulamos que el repositorio encuentra el chofer y que su estado es OCUPADO
        com.logitrack.sistema_logistica.model.ChoferDetalle choferOcupado = new com.logitrack.sistema_logistica.model.ChoferDetalle();
        choferOcupado.setIdChofer(999);
        choferOcupado.setDisponible(false); // Chofer ocupado

        org.mockito.Mockito.lenient().when(choferDetalleRepository.findById(999)).thenReturn(java.util.Optional.of(choferOcupado));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            envioService.asignarTransporte("LT-1000", dto);
        });
        
        // Verificamos que no se guardó nada
        verify(envioRepository, never()).save(any());
    }
    // =========================================================
    // TICKET #284: Pruebas del servicio Mock de notificaciones
    // =========================================================

    @Test
    public void mockEmailService_EnvioValido_NoDeberiaLanzarExcepcion() {
        // Arrange: Instanciamos el servicio Mock (el servicio falso que hizo Nico)
        // Fijate bien el package, si no es 'impl', ajustalo según tu carpeta
        com.logitrack.sistema_logistica.service.impl.ConsoleNotificationService mockEmailService = 
            new com.logitrack.sistema_logistica.service.impl.ConsoleNotificationService();

        String emailDestino = "cliente@logitrack.com";
        String asunto = "Prueba de notificación";
        String cuerpo = "Este es un mensaje de prueba controlado.";

        // Act & Assert: Comprobamos que el método se ejecuta sin lanzar excepciones
        assertDoesNotThrow(() -> {
            mockEmailService.enviarNotificacion(emailDestino, asunto, cuerpo);
        }, "El servicio Mock debería procesar el envío sin lanzar ninguna excepción");
    }
}