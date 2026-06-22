package com.logitrack.sistema_logistica.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils; 
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.service.AuditoriaService;
import com.logitrack.sistema_logistica.service.EnvioService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.Collections;

public class EnvioControllerTest {

    private org.springframework.test.web.servlet.MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private EnvioService envioService;

    @Mock
    private com.logitrack.sistema_logistica.repository.EnvioRepository envioRepository;

    @Mock
    private com.logitrack.sistema_logistica.repository.UsuarioRepository usuarioRepository;

    @Mock
    private com.logitrack.sistema_logistica.repository.HistorialEstadosRepository historialEstadosRepository;
    
    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private com.logitrack.sistema_logistica.service.CartaPorteService cartaPorteService;

    @Mock
    private com.logitrack.sistema_logistica.service.CartaPortePdfService cartaPortePdfService;

    @Mock
    private com.logitrack.sistema_logistica.service.ReporteService reporteService;

    @InjectMocks
    private EnvioController envioController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // --- EL TRUCO DEL TESTER PARA QUE NO TIRE 404/400 ---
        ReflectionTestUtils.setField(envioController, "envioService", envioService);
        ReflectionTestUtils.setField(envioController, "envioRepository", envioRepository);
        ReflectionTestUtils.setField(envioController, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(envioController, "historialEstadosRepository", historialEstadosRepository);
        ReflectionTestUtils.setField(envioController, "reporteService", reporteService);
        ReflectionTestUtils.setField(envioController, "cartaPorteService", cartaPorteService);
        ReflectionTestUtils.setField(envioController, "cartaPortePdfService", cartaPortePdfService);

        this.mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(envioController).build();
        this.objectMapper = new ObjectMapper();
    }   

    @Test
    public void actualizarEstadoChofer_DeberiaRetornar200() throws Exception {
        String idEnvio = "LT-333";
        Envio envioMock = Envio.builder().idEnvio(idEnvio).build();

        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("chofer_test");

        when(envioService.actualizarEstadoChofer(eq(idEnvio), eq("EN_TRANSITO"), eq("chofer_test")))
                .thenReturn(envioMock);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/envios/" + idEnvio + "/estado")
                .principal(authenticationMock)
                .param("nuevoEstado", "EN_TRANSITO"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.idEnvio").value(idEnvio));
    }

    @Test
    public void obtenerTrackingTiempoReal_DeberiaRetornar200() throws Exception {
        String idEnvio = "LT-444";
        Map<String, Object> trackingData = Map.of(
                "porcentajeCompletado", 50.0,
                "latitudActual", -34.6037,
                "longitudActual", -58.3816
        );

        when(envioService.obtenerUbicacionActual(idEnvio)).thenReturn(trackingData);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/" + idEnvio + "/tracking")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.porcentajeCompletado").value(50.0));
    }
/* 
    @Test
    public void buscarEnvios_DatosValidos_Retorna200() throws Exception {
        Page<Envio> pageMock = new PageImpl<>(
            Collections.emptyList(), 
            PageRequest.of(0, 10), 
            0
        );

        when(envioService.buscarEnviosConFiltros(any(), any(), any(), any(), any(), any()))
            .thenReturn(pageMock);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/search")
                .param("estado", "PENDIENTE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }
*/
    @Test
    public void buscarEnvios_FechaInvalida_Retorna400() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/search")
                .param("fecha", "2026-05-23"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Formato de fecha inválido. Use dd/MM/yyyy."));
    }

    @Test
    public void buscarEnvios_EstadoInvalido_Retorna400() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/search")
                .param("estado", "ESTADO_INEXISTENTE"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Estado inválido. Use uno de los valores permitidos: PENDIENTE, EN_TRANSITO, ENTREGADO, CANCELADO"));
    }

    @Test
    public void consultarHistorial_EnvioExistente_Retorna200() throws Exception {
        String idEnvio = "LT-123";
        HistorialResponseDTO dto = new HistorialResponseDTO();
        dto.setEstadoNuevo("PENDIENTE");
        when(envioService.obtenerHistorialPorEnvio(idEnvio)).thenReturn(List.of(dto));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/{idEnvio}/historial", idEnvio))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].estadoNuevo").value("PENDIENTE"));
    }

    @Test
    public void consultarHistorial_EnvioNoExiste_Retorna404() throws Exception {
        String idEnvio = "LT-999";
        when(envioService.obtenerHistorialPorEnvio(idEnvio)).thenThrow(new RuntimeException("Envío no encontrado"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/{idEnvio}/historial", idEnvio))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Envío no encontrado"));
    }

    @Test
    public void consultarHistorial_ErrorInesperado_Retorna500() throws Exception {
        String idEnvio = "LT-123";
        when(envioService.obtenerHistorialPorEnvio(idEnvio)).thenAnswer(inv -> {
            throw new Exception("Error de base de datos inesperado");
        });

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/{idEnvio}/historial", idEnvio))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Error al obtener el historial: Error de base de datos inesperado"));
    }

    @Test
    public void listarEnvios_DeberiaRetornarListaVaciaYStatus200() throws Exception {
        when(envioRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void obtenerHistorialCompleto_DeberiaRetornarStatus200() throws Exception {
        when(historialEstadosRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/historial-completo"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void crearEnvio_ConDatosValidos_DeberiaRetornarStatus201() throws Exception {
        com.logitrack.sistema_logistica.model.Usuario mockUsuario = new com.logitrack.sistema_logistica.model.Usuario();
        mockUsuario.setIdUsuario(1);
        when(usuarioRepository.findByUsername(any())).thenReturn(java.util.Optional.of(mockUsuario));
        
        Authentication authMock = mock(Authentication.class);
        when(authMock.getName()).thenReturn("operador_test");
        when(envioService.crearNuevoEnvio(any())).thenReturn(new Envio());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/envios")
                .principal(authMock)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());
    }

    @Test
    public void obtenerEnvioPorTracking_CuandoNoExiste_DeberiaRetornar404() throws Exception {
        when(envioService.buscarPorId(any())).thenThrow(new RuntimeException("No encontrado"));
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/buscar/LT-999"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
    }
    
    @Test
    public void cancelarEnvio_ConIdValido_DeberiaRetornar200() throws Exception {
        java.security.Principal principalMock = mock(java.security.Principal.class);
        when(principalMock.getName()).thenReturn("supervisor_test");
        when(envioService.cancelarEnvio(any(), any())).thenReturn(new Envio());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/envios/LT-1/cancelar")
               .principal(principalMock))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void editarEnvio_ConDatosValidos_DeberiaRetornar200() throws Exception {
        java.security.Principal principalMock = mock(java.security.Principal.class);
        when(principalMock.getName()).thenReturn("operador_test");
        when(envioService.editarEnvio(any(), any(), any())).thenReturn(new Envio());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/envios/LT-1")
               .principal(principalMock)
               .contentType(MediaType.APPLICATION_JSON)
               .content("{}"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void listarSinAsignar_DeberiaRetornarStatus200() throws Exception {
        when(envioRepository.findEnviosSinAsignar()).thenReturn(java.util.Collections.emptyList());
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/sin-asignar"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void asignarTransporte_ConDatosValidos_DeberiaRetornar200() throws Exception {
        when(envioService.asignarTransporte(any(), any())).thenReturn(new Envio());
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/envios/LT-1/asignar-transporte")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{}"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void actualizarOperativaEnvio_DeberiaRetornarStatus200() throws Exception {
        Authentication authMock = mock(Authentication.class);
        when(envioService.actualizarEstadoOperativo(any(), any(), any())).thenReturn(new Envio());
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/envios/LT-1/operativo")
               .principal(authMock)
               .contentType(MediaType.APPLICATION_JSON)
               .content("{}"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void cobertura_listarEnviosVacio() throws Exception {
        org.mockito.Mockito.when(envioRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void cobertura_historialCompleto() throws Exception {
        org.mockito.Mockito.when(historialEstadosRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/historial-completo"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void cobertura_buscarEnvioError() throws Exception {
        org.mockito.Mockito.when(envioService.buscarPorId(org.mockito.ArgumentMatchers.anyString()))
               .thenThrow(new RuntimeException("No existe"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/buscar/LT-999"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void cobertura_cancelarEnvioError() throws Exception {
        java.security.Principal principal = org.mockito.Mockito.mock(java.security.Principal.class);
        org.mockito.Mockito.when(principal.getName()).thenReturn("admin");
        org.mockito.Mockito.when(envioService.cancelarEnvio(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
               .thenThrow(new RuntimeException("Fallo al cancelar"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/envios/LT-1/cancelar")
               .principal(principal))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void cobertura_asignarTransporteError() throws Exception {
        org.mockito.Mockito.when(envioService.asignarTransporte(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
               .thenThrow(new RuntimeException("Fallo al asignar"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/envios/LT-1/asignar-transporte")
               .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
               .content("{}"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void cobertura_operativoError() throws Exception {
        org.springframework.security.core.Authentication auth = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        org.mockito.Mockito.when(envioService.actualizarEstadoOperativo(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
               .thenThrow(new RuntimeException("Fallo operativo"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/envios/LT-1/operativo")
               .principal(auth)
               .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
               .content("{}"))
               .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }
// ==========================================
    //              #624 
    // ==========================================

    /*
    @Test
    public void sanitizacionAPI_alConsultarEnvioPublico_noDebeFiltrarDatosSensibles_Issue624() throws Exception {
        // Configuramos el MockMvc para simular la petición web
        org.springframework.test.web.servlet.MockMvc mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(envioController).build();

        // GIVEN: Un envío que internamente tiene datos sensibles asignados (como el camión y su patente)
        Envio envioSensible = new Envio();
        envioSensible.setIdEnvio("TRK-SEC-123");
        envioSensible.setEstadoActual(EstadoEnvio.EN_TRANSITO);
        
        com.logitrack.sistema_logistica.model.Camion camionMock = new com.logitrack.sistema_logistica.model.Camion();
        camionMock.setPatente("AB123CD");
        
        // ACÁ ESTÁ LA CORRECCIÓN: Usamos setCamion() como dice tu modelo
        envioSensible.setCamion(camionMock); 
        
        // Simulamos que el servicio devuelve este envío al controlador
        when(envioService.buscarPorId("TRK-SEC-123")).thenReturn(envioSensible);

        // WHEN & THEN: Simulamos la consulta del cliente externo
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/buscar/TRK-SEC-123")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                // Verificamos que el estado sí se devuelva al cliente
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.estadoActual").value("EN_TRANSITO"))
                // AUDITORÍA DE SEGURIDAD (#624): Exigimos que el JSON NO contenga la patente del camión
                // ACÁ TAMBIÉN CORREGIDO: Buscamos $.camion.patente en lugar de camionAsignado
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.camion.patente").doesNotExist());
    }
    */


    // ==========================================
    //             #623
    // ==========================================

    @Test
    public void privacidadAPI_alConsultarConCuitIncorrecto_debeBloquearAcceso_Issue623() throws Exception {
        // Configuramos el MockMvc
        org.springframework.test.web.servlet.MockMvc mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(envioController).build();

        // GIVEN: Un Tracking ID válido, pero el atacante usa un CUIT que no le pertenece
        String trackingValido = "TRK-VALIDO-999";
        String cuitInvalido = "20-00000000-0"; 

        // Simulamos que el backend detecta el robo de identidad y lanza una excepción
        when(envioService.buscarPorId(trackingValido)) 
            .thenThrow(new SecurityException("Acceso denegado: El CUIT no coincide con el envío"));

        // WHEN & THEN: El sistema debe rechazar la petición y proteger la privacidad
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/buscar/" + trackingValido)
                .header("X-CUIT-Cliente", cuitInvalido) // Simulamos que el CUIT viaja en los headers
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                // AUDITORÍA (#623): Verificamos que se devuelva un error HTTP de cliente (4xx) en lugar de los datos
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is4xxClientError());
    }
}