package com.logitrack.sistema_logistica.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Nota: Eliminamos @WebMvcTest y @AutoConfigureMockMvc
class HealthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Configuramos MockMvc de forma aislada solo para este controlador,
        // esquivando por completo la carga de la configuración de Spring.
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController()).build();
    }

    @Test
    @DisplayName("Debe retornar HTTP 200 y el texto 'OK' al consultar /api/health")
    void healthCheck_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}