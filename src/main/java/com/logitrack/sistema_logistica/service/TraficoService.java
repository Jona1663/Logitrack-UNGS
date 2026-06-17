package com.logitrack.sistema_logistica.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TraficoService {

    @Value("${tomtom.api.key}")
    private String apiKey;

    @Value("${tomtom.api.url:https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String obtenerNivelTrafico(Double lat, Double lon) {
        if (lat == null || lon == null) {
            log.warn("[Trafico] Coordenadas nulas, usando default 'moderado'");
            return "moderado";
        }

        try {
            String url = apiUrl + "?point=" + lat + "," + lon + "&key=" + apiKey;

            Map response = restTemplate.getForObject(url, Map.class);

            Map<?, ?> data = (Map<?, ?>) response.get("flowSegmentData");
            if (data == null) return "moderado";

            double freeFlow    = toDouble(data.get("freeFlowSpeed"));
            double currentSpeed = toDouble(data.get("currentSpeed"));

            if (freeFlow <= 0) return "moderado";

            // Porcentaje es qué tan lento va respecto a la velocidad libre
            double congestion = 1.0 - (currentSpeed / freeFlow);
            String resultado = mapearNivelTrafico(congestion);

            log.info("[Trafico] Coordenadas ({},{}) → libre:{}km/h actual:{}km/h congestión:{:.0%} → {}",
                    lat, lon, freeFlow, currentSpeed, congestion, resultado);

            return resultado;

        } catch (Exception e) {
            log.warn("[Trafico] TomTom no disponible: {}. Usando default 'moderado'", e.getMessage());
            return "moderado";
        }
    }

    /**
     * Mapea el porcentaje de congestión a los niveles que acepta el modelo ML.
     * 0%–20%  → bajo       (tráfico fluido)
     * 20%–45% → moderado   (algo de demora)
     * 45%–70% → alto       (demora significativa)
     * 70%+    → muy_alto   (tráfico muy denso / casi detenido)
     */
    private String mapearNivelTrafico(double congestion) {
        if (congestion < 0.20) return "bajo";
        if (congestion < 0.45) return "moderado";
        if (congestion < 0.70) return "alto";
        return "muy_alto";
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}