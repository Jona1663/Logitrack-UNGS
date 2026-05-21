package com.logitrack.sistema_logistica.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logitrack.sistema_logistica.service.EnvioService;

public class EnvioControllerTest {

    // Usamos el MockMvc puro, sin cargar el contexto completo de Spring
    private MockMvc mockMvc;

    // Usamos el @Mock tradicional de Mockito en vez del @MockBean problemático
    @Mock
    private EnvioService envioService;

    // Inyectamos el servicio falso directo en tu controlador
    @InjectMocks
    private EnvioController envioController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // ¡ESTA ES LA MAGIA! Levanta la API simulada solo para este archivo, 
        // saltándose todos los filtros de seguridad y los roles.
        mockMvc = MockMvcBuilders.standaloneSetup(envioController).build();
    }
    //issue 218 //
    @Test
    public void testObtenerRutaCompleta_ParaMapaInteractivo() throws Exception {
        
        // 1. PREPARACIÓN: Armamos un JSON "de mentira" con coordenadas de Origen y Destino
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode coordenadasDePrueba = mapper.createArrayNode();
        coordenadasDePrueba.add(mapper.createArrayNode().add(-58.4).add(-34.6));
        coordenadasDePrueba.add(mapper.createArrayNode().add(-57.9).add(-34.9));

        // Le decimos al servicio falso que devuelva esas coordenadas
        when(envioService.obtenerGeometriaRuta("LT-MAPA")).thenReturn(coordenadasDePrueba);

        // 2. EJECUCIÓN Y VERIFICACIÓN: Hacemos la petición GET a la ruta
        mockMvc.perform(get("/api/envios/LT-MAPA/ruta-completa"))
               // Verificamos que responda HTTP 200 OK
               .andExpect(status().isOk())
               // Verificamos que mande el ID correcto
               .andExpect(jsonPath("$.idEnvio").value("LT-MAPA"))
               // Verificamos que las coordenadas existan en la respuesta
               .andExpect(jsonPath("$.coordinates").exists());
    }
}