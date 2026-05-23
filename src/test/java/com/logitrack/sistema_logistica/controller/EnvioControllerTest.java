package com.logitrack.sistema_logistica.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.service.AuditoriaService;
import com.logitrack.sistema_logistica.service.EnvioService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.Collections;

public class EnvioControllerTest {

    // Instancia del Postman "fantasma"
    private org.springframework.test.web.servlet.MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // Usamos @Mock nativo de Mockito en lugar de @MockBean
    @Mock
    private EnvioService envioService;

    @Mock
    private AuditoriaService auditoriaService;

    // Inyectamos los mocks directamente en el controlador limpio
    @InjectMocks
    private EnvioController envioController;

    @BeforeEach
    void setUp() {
        // Inicializamos los mocks antes de cada test
        MockitoAnnotations.openMocks(this);
        
        // Configuramos el MockMvc de forma "standalone" como hicieron tus compañeros
        this.mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(envioController).build();
        this.objectMapper = new ObjectMapper();
    }

    // ==========================================
    // TESTS DEL CONTROLADOR
    // ==========================================

  /*  @Test
    public void crearEnvio_DeberiaRetornar201() throws Exception {
        // Arrange
        EnvioRequestDTO dto = new EnvioRequestDTO();
        dto.setCpe("12345678");

        Envio envioMock = Envio.builder().idEnvio("LT-001").build();

        // Como no usamos @WithMockUser, simulamos la autenticación manualmente
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("operador_juan");

        when(envioService.crearNuevoEnvio(any(EnvioRequestDTO.class))).thenReturn(envioMock);

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/envios")
                .principal(authenticationMock) // Pasamos el usuario simulado aquí
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.idEnvio").value("LT-001"));
    }*/

   /*  @Test
    public void listarEnvios_DeberiaRetornar200() throws Exception {
        // Arrange
        Envio envio1 = Envio.builder().idEnvio("LT-111").build();
        Envio envio2 = Envio.builder().idEnvio("LT-222").build();
        
        when(envioService.obtenerTodos()).thenReturn(List.of(envio1, envio2));

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.length()").value(2));
    }*/

    @Test
    public void actualizarEstadoChofer_DeberiaRetornar200() throws Exception {
        // Arrange
        String idEnvio = "LT-333";
        Envio envioMock = Envio.builder().idEnvio(idEnvio).build();

        // Simulamos la autenticación del chofer
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("chofer_test");

        when(envioService.actualizarEstadoChofer(eq(idEnvio), eq("EN_TRANSITO"), eq("chofer_test")))
                .thenReturn(envioMock);

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/envios/" + idEnvio + "/estado")
                .principal(authenticationMock) // Inyectamos la sesión
                .param("nuevoEstado", "EN_TRANSITO"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.idEnvio").value(idEnvio));
    }

    @Test
    public void obtenerTrackingTiempoReal_DeberiaRetornar200() throws Exception {
        // Arrange
        String idEnvio = "LT-444";
        Map<String, Object> trackingData = Map.of(
                "porcentajeCompletado", 50.0,
                "latitudActual", -34.6037,
                "longitudActual", -58.3816
        );

        when(envioService.obtenerUbicacionActual(idEnvio)).thenReturn(trackingData);

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/" + idEnvio + "/tracking")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.porcentajeCompletado").value(50.0));
    }

        @Test
        public void buscarEnvios_DatosValidos_Retorna200() throws Exception {
            // Arrange

            Page<Envio> pageMock = new PageImpl<>(
            Collections.emptyList(), 
            PageRequest.of(0, 10), // Esto crea un Pageable real con tamaño 10
            0
        );

        when(envioService.buscarEnviosConFiltros(any(), any(), any(), any(), any(), any()))
            .thenReturn(pageMock);
          //  when(envioService.buscarEnviosConFiltros(any(), any(), any(), any(), any(), any()))
           //     .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList()));

            // Act & Assert
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/search")
                    .param("estado", "PENDIENTE")
                    .contentType(MediaType.APPLICATION_JSON))
                    // Esta línea captura la excepción resuelta y la tira a System.err
                    .andDo(result -> {
                        if (result.getResolvedException() != null) {
                            System.err.println("--- ERROR CAPTURADO ---");
                            result.getResolvedException().printStackTrace();
                        }
                    })
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        }

    @Test
    public void buscarEnvios_FechaInvalida_Retorna400() throws Exception {
        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/search")
                .param("fecha", "2026-05-23")) // Formato incorrecto (espera dd/MM/yyyy)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Formato de fecha inválido. Use dd/MM/yyyy."));
    }

    @Test
    public void buscarEnvios_EstadoInvalido_Retorna400() throws Exception {
        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/search")
                .param("estado", "ESTADO_INEXISTENTE"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Estado inválido. Use uno de los valores permitidos: PENDIENTE, EN_TRANSITO, ENTREGADO, CANCELADO"));
    }
    @Test
    public void consultarHistorial_EnvioExistente_Retorna200() throws Exception {
        // Arrange
        String idEnvio = "LT-123";
        HistorialResponseDTO dto = new HistorialResponseDTO();
        dto.setEstadoNuevo("PENDIENTE");
        when(envioService.obtenerHistorialPorEnvio(idEnvio)).thenReturn(List.of(dto));

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/{idEnvio}/historial", idEnvio))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].estadoNuevo").value("PENDIENTE"));
    }

    @Test
    public void consultarHistorial_EnvioNoExiste_Retorna404() throws Exception {
        // Arrange
        String idEnvio = "LT-999";
        when(envioService.obtenerHistorialPorEnvio(idEnvio)).thenThrow(new RuntimeException("Envío no encontrado"));

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/{idEnvio}/historial", idEnvio))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Envío no encontrado"));
    }

    @Test
    public void consultarHistorial_ErrorInesperado_Retorna500() throws Exception {
        // Arrange
        String idEnvio = "LT-123";
        // Simulamos un error genérico que no es RuntimeException
        when(envioService.obtenerHistorialPorEnvio(idEnvio)).thenAnswer(inv -> {
            throw new Exception("Error de base de datos inesperado");
        });

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/envios/{idEnvio}/historial", idEnvio))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Error al obtener el historial: Error de base de datos inesperado"));
    }

}