package com.logitrack.sistema_logistica.service;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.logitrack.sistema_logistica.dto.EnvioOperativoDTO;
import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
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
    @Mock private RestTemplate restTemplate;
    @Mock private GraphHopperService graphHopperService;
    @Mock private RutaEnvioRepository rutaEnvioRepository;
    @Mock private HistorialEstadosRepository historialRepository;

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


    @Test
    public void obtenerDetalleConETA_DeberiaCalcularElTiempoCorrectamente() {
        // Arrange
        String idEnvio = "LT-123";
        // Supongamos que el camión salió a las 10:00 AM
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
        // Si la distancia es exactamente 65.0 km, a 65 km/h debería tardar exactamente 60 minutos.
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

        // Act
        com.logitrack.sistema_logistica.dto.EnvioDetalleResponseDTO detalle = 
            envioService.obtenerDetalleConETA(idEnvio);

        // Assert
        assertNotNull(detalle);
        System.out.print(detalle);
        System.out.println("ETA en el DTO: " + detalle.getFechaEstimadaLlegada());
        // Validamos que el ETA calculado sea exactamente a las 11:00 AM (10:00 + 60 mins)
        assertEquals(java.time.LocalDateTime.of(2026, 5, 20, 11, 0), detalle.getFechaEstimadaLlegada());
    }


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
                
        when(historialRepository.buscarHistorialPorEnvio(idEnvio))
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
        // El envío en BD está PENDIENTE y con Prioridad ALTA
        Envio envioExistente = Envio.builder()
                .estadoActual(EstadoEnvio.PENDIENTE)
                .prioridadIa("ALTA")
                .distanciaKm(65.0)
                //.fechaSalida(fechaSalida)
                .origen(origen)
                .destino(destino)
                .chofer(chofer)
                .camion(camion)
                .build();
                
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioExistente));
        
        // El DTO del request manda exactamente lo mismo
        EnvioOperativoDTO dto = new EnvioOperativoDTO();
        dto.setEstado(EstadoEnvio.PENDIENTE);
        dto.setPrioridadIa("ALTA");
        
        // Necesitamos mockear el Auth aunque no haga nada, para evitar NullPointer
        org.springframework.security.core.Authentication authMock = mock(org.springframework.security.core.Authentication.class);
        
        when(envioRepository.save(any(Envio.class))).thenReturn(envioExistente);

        // 1. Mockea el Auth para que devuelva un username
        when(authMock.getName()).thenReturn("operador1");

        // 2. Mockea el repositorio para que encuentre el usuario cuando lo busca
        Usuario usuarioMock = new Usuario(); 
        when(usuarioRepository.findByUsername("operador1")).thenReturn(Optional.of(usuarioMock));



        // Act
        Envio resultado = envioService.actualizarEstadoOperativo(idEnvio, dto, authMock);

        // Assert
        assertEquals(EstadoEnvio.PENDIENTE, resultado.getEstadoActual());
        
        // LA CLAVE: Verificamos que el repositorio de historial NUNCA fue llamado
        verify(historialEstadosRepository, never()).save(any(HistorialEstados.class));
    }
*/


@Test
    public void actualizarEstadoYPrioridad_DeberiaActualizarYGuardarHistorial() {
        // Arrange
        String idEnvio = "LT-123";
// --- INICIALIZACIÓN COMPLETA ---
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
                .origen(origen)   // <--- ¡Esto evita el NullPointer!
                .destino(destino) // <--- ¡Esto evita el NullPointer!
                .build();
        Usuario user = new Usuario();

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);
        // Mock necesario para el ruteo
        when(graphHopperService.obtenerRuta(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(new com.fasterxml.jackson.databind.node.ObjectNode(
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance));

        // Act
        envioService.actualizarEstadoYPrioridad(idEnvio, "EN_TRANSITO", "ALTA", user, TipoEvento.CAMBIO_ESTADO);
        // Act
        envioService.actualizarEstadoYPrioridad(idEnvio, "EN_TRANSITO", "ALTA", user, TipoEvento.CAMBIO_ESTADO);

        // Assert
        assertEquals(EstadoEnvio.EN_TRANSITO, envio.getEstadoActual());
        assertEquals("ALTA", envio.getPrioridadIa());
        verify(historialEstadosRepository, times(2)).save(any(HistorialEstados.class));
    }

    @Test
    public void editarEnvio_DeberiaActualizarDatosYGuardarHistorial() {
        // Arrange
        String idEnvio = "LT-123";
        EnvioRequestDTO dto = new EnvioRequestDTO();
        dto.setIdChofer(1);
        dto.setPatenteCamion("ABC-123");
        
        Envio envio = Envio.builder().estadoActual(EstadoEnvio.PENDIENTE).build();
        
// --- INICIALIZACIÓN COMPLETA ---
        ChoferDetalle chofer = new ChoferDetalle();
        chofer.setVtoLicencia(java.time.LocalDate.now().plusDays(10)); // Fecha futura
        chofer.setVtoLinti(java.time.LocalDate.now().plusDays(10));
        
        Camion camion = new Camion();
        camion.setVtoSenasa(java.time.LocalDate.now().plusDays(10));

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));
        when(choferDetalleRepository.findById(1)).thenReturn(Optional.of(chofer)); // <--- Chofer con fechas
        when(camionRepository.findById("ABC-123")).thenReturn(Optional.of(camion)); // <--- Camion con fechas
        when(usuarioRepository.findByUsername("user")).thenReturn(Optional.of(new Usuario()));
        when(envioRepository.save(any(Envio.class))).thenReturn(envio);

        // Act
        envioService.editarEnvio(idEnvio, dto, "user");

        // Assert
        verify(envioRepository, times(1)).save(any(Envio.class));
        verify(historialEstadosRepository, times(1)).save(any(HistorialEstados.class));
    }

    @Test
    public void obtenerUbicacionActual_DeberiaLanzarExcepcionSiRutaEsNula() {
        // Arrange
        String idEnvio = "LT-123";
        Envio envio = Envio.builder()
                .estadoActual(EstadoEnvio.EN_TRANSITO)
                .rutaEnvio(null) // No tiene ruta generada
                .build();
        
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envio));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            envioService.obtenerUbicacionActual(idEnvio)
        );
        assertTrue(ex.getMessage().contains("No se encontraron datos de ruta"));
    }

   
   //issue #264
    @Test
    public void intentarAccion_EnViajeFinalizado_DeberiaLanzarExcepcion() {
        // Arrange: Simulamos un viaje que ya está terminado (ENTREGADO)
        String idEnvio = "LT-FINALIZADO-99";
        Envio envioFinalizado = new Envio();
        envioFinalizado.setIdEnvio(idEnvio);
        envioFinalizado.setEstadoActual(EstadoEnvio.ENTREGADO); 
        
        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioFinalizado));

        EnvioRequestDTO dtoFalso = new EnvioRequestDTO(); 

        // Act & Assert: Intentamos meterle un cambio al viaje y esperamos que el sistema lo rechace
        // Usamos editarEnvio porque ES REAL, YA EXISTE y tiene la regla de bloqueo programada
        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            envioService.editarEnvio(idEnvio, dtoFalso, "tester_qa");
        });

        // Validamos que el sistema efectivamente frenó la acción y tiró error
        assertTrue(excepcion.getMessage().contains("No se pueden modificar los datos"));
        
        // Verificamos que el sistema frenó todo y NUNCA guardó nada en la BD
        verify(envioRepository, never()).save(any());
    }
    ////issue 291
    @Test
    public void obtenerUbicacionActual_DeberiaLimitarAlDestinoSiElTiempoExcedioLaDuracion() {
        // Arrange
        String idEnvio = "LT-TEST-LIMITE";
        long duracionTotal = 3600L; // 1 hora de viaje esperada
        
        // Simulación: El camión salió hace 2 horas. Es decir, se pasó del tiempo esperado.
        // Esto va a forzar que el IF de límite (segundosTranscurridos > duracionTotal) se ejecute (verde en JaCoCo)
        java.time.LocalDateTime fechaSalidaSimulada = java.time.LocalDateTime.now().minusSeconds(duracionTotal + 1800);

        com.logitrack.sistema_logistica.model.RutaEnvio rutaMock = new com.logitrack.sistema_logistica.model.RutaEnvio();
        rutaMock.setDuracionTotalSegundos(duracionTotal);
        // Formato esperado por ObjectMapper de tu código: [[lon, lat], [lon, lat], [lon, lat]]
        rutaMock.setPolylineJson("[[-58.0, -34.0], [-58.5, -34.5], [-59.0, -35.0]]");

        Envio envioMock = Envio.builder()
                .idEnvio(idEnvio)
                .estadoActual(EstadoEnvio.EN_TRANSITO)
                .fechaSalida(fechaSalidaSimulada)
                .rutaEnvio(rutaMock)
                .build();

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioMock));

        // Act
        // Llamamos al servicio, simulando que pedimos la ubicación mucho después de llegar
        Map<String, Object> ubicacion = envioService.obtenerUbicacionActual(idEnvio);

        // Assert
        assertNotNull(ubicacion);
        // Verifica que el porcentaje quedó topeado en 100% y no tiró error
        assertEquals(100.0, (Double) ubicacion.get("porcentajeCompletado"), "Debería limitar el progreso al 100%");
        // Verifica que devolvió EXACTAMENTE la última coordenada del JSON (Destino final)
        assertEquals(-35.0, (Double) ubicacion.get("latitudActual"), 0.0001);
        assertEquals(-59.0, (Double) ubicacion.get("longitudActual"), 0.0001);
    }

    @Test
    public void obtenerUbicacionActual_DeberiaCalcularPosicionIntermediaCorrectamente() {
        // Arrange
        String idEnvio = "LT-TEST-MITAD";
        long duracionTotal = 3600L; // 1 hora de viaje
        
        // Simulación: El camión salió hace exactamente 30 minutos (mitad del viaje)
        java.time.LocalDateTime fechaSalidaSimulada = java.time.LocalDateTime.now().minusSeconds(1800);

        com.logitrack.sistema_logistica.model.RutaEnvio rutaMock = new com.logitrack.sistema_logistica.model.RutaEnvio();
        rutaMock.setDuracionTotalSegundos(duracionTotal);
        // Ruta recta horizontal para probar interpolación: inicia en -58.0 y termina en -59.0
        rutaMock.setPolylineJson("[[-58.0, -34.0], [-59.0, -34.0]]");

        Envio envioMock = Envio.builder()
                .idEnvio(idEnvio)
                .estadoActual(EstadoEnvio.EN_REPARTO)
                .fechaSalida(fechaSalidaSimulada)
                .rutaEnvio(rutaMock)
                .build();

        when(envioRepository.findById(idEnvio)).thenReturn(Optional.of(envioMock));

        // Act
        Map<String, Object> ubicacion = envioService.obtenerUbicacionActual(idEnvio);

        // Assert
        assertNotNull(ubicacion);
        double porcentaje = (Double) ubicacion.get("porcentajeCompletado");
        
        // El porcentaje debe ser cercano al 50% (puede variar una fracción de milisegundo por la ejecución)
        assertTrue(porcentaje >= 49.9 && porcentaje <= 50.1, "El camión debe estar al 50% de la ruta");
        
        Double longitudActual = (Double) ubicacion.get("longitudActual");
        // Verifica geométricamente que el punto esté a la mitad del trayecto
        assertTrue(longitudActual < -58.0 && longitudActual > -59.0, "La coordenada debe ubicarse entre el punto inicial y final");
    }
}

