package com.logitrack.sistema_logistica.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.Envio;

@ExtendWith(MockitoExtension.class)
public class ETAClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ETAClientService etaClientService;

    @BeforeEach
    void setUp() {
        // FORZAMOS la inyección del mock en el servicio para que no use el del constructor
        ReflectionTestUtils.setField(etaClientService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(etaClientService, "etaServiceUrl", "http://localhost:8000");
    }

    @Test
    public void calcularEta_RespuestaExitosa_DeberiaRetornarResult() {
        Envio envio = new Envio();
        envio.setDistanciaKm(100.0);
        Camion camion = new Camion();
        camion.setCapacidadCargaKg(10000);

        Map<String, Object> responseBody = Map.of(
            "eta_horas", 2.5,
            "eta_minutos", 150,
            "eta_formateado", "2h 30min",
            "confianza", "alta"
        );
        
        // Mockeamos el postForEntity
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(responseBody));

        ETAClientService.ETAResult result = etaClientService.calcularEta(envio, camion, "despejado", "bajo");

        assertNotNull(result);
        assertTrue(result.disponible, "El resultado debería estar disponible");
        assertEquals(2.5, result.etaHoras);
    }

    @Test
    public void calcularEta_ServicioFalla_DeberiaRetornarPendiente() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenThrow(new RuntimeException("Error"));

        ETAClientService.ETAResult result = etaClientService.calcularEta(new Envio(), new Camion(), "lluvia", "alto");

        assertFalse(result.disponible);
    }
}