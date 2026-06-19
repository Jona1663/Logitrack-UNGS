package com.logitrack.sistema_logistica.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.Envio;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ETAClientService {

    @Value("${eta.service.url:http://localhost:8000}")
    private String etaServiceUrl;

    private final RestTemplate restTemplate;

    public ETAClientService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    // Resultado de la predicción 

    public static class ETAResult {
        public final double etaHoras;
        public final int etaMinutos;
        public final String etaFormateado;  // "4h 35min"
        public final String confianza;      // "alta" | "media" | "baja"
        public final boolean disponible;    // false si el microservicio falló

        private ETAResult(double etaHoras, int etaMinutos, String etaFormateado,
                          String confianza, boolean disponible) {
            this.etaHoras = etaHoras;
            this.etaMinutos = etaMinutos;
            this.etaFormateado = etaFormateado;
            this.confianza = confianza;
            this.disponible = disponible;
        }

        /** Escenario 3: el servicio no respondió, la asignación continúa igual. */
        public static ETAResult pendiente() {
            return new ETAResult(0, 0, "Pendiente de cálculo", "", false);
        }

        public static ETAResult of(double horas, int minutos, String formateado, String confianza) {
            return new ETAResult(horas, minutos, formateado, confianza, true);
        }
    }

    // Método principal 
    public ETAResult calcularEta(Envio envio, Camion camion, String clima, String trafico) {
        try {
            Map<String, Object> payload = buildPayload(envio, camion, clima, trafico);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    etaServiceUrl + "/api/v1/eta/predict",
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> body = response.getBody();
                double etaHoras   = toDouble(body.get("eta_horas"));
                int etaMinutos    = toInt(body.get("eta_minutos"));
                String formateado = String.valueOf(body.get("eta_formateado"));
                String confianza  = String.valueOf(body.get("confianza"));

                log.info("[ETA-ML] {} para {}km | clima={} trafico={} confianza={}",
                        formateado, envio.getDistanciaKm(), clima, trafico, confianza);

                return ETAResult.of(etaHoras, etaMinutos, formateado, confianza);
            }

            log.warn("[ETA-ML] Respuesta inesperada: {}", response.getStatusCode());
            return ETAResult.pendiente();

        } catch (Exception e) {
            // Escenario 3: timeout, red caída
            log.warn("[ETA-ML] Microservicio no disponible: {}. Usando fallback.", e.getMessage());
            return ETAResult.pendiente();
        }
    }

    // Construcción del payload 

    private Map<String, Object> buildPayload(Envio envio, Camion camion,
                                              String clima, String trafico) {
        LocalDateTime ahora = LocalDateTime.now();
        return Map.of(
                "distancia_km",        envio.getDistanciaKm() != null ? envio.getDistanciaKm() : 100.0,
                "tipo_ruta",           inferirTipoRuta(envio),
                "condicion_climatica", clima,
                "nivel_trafico",       trafico,
                "tipo_vehiculo",       inferirTipoVehiculo(camion),
                "cantidad_paradas",    0,
                "es_hora_pico",        esHoraPico(ahora) ? 1 : 0,
                "es_fin_de_semana",    esFinDeSemana(ahora) ? 1 : 0,
                "carga_kg",            envio.getKgOrigen() != null ? envio.getKgOrigen().doubleValue() : 5000.0,
                "tiene_refrigeracion", 0
        );
    }

    //Tipo de ruta desde la distancia del envío.
    //Envio no tiene campo tipoRuta, así que usamos la distancia

    private String inferirTipoRuta(Envio envio) {
        if (envio.getDistanciaKm() == null) return "mixta";
        double km = envio.getDistanciaKm();
        if (km > 300) return "autopista";
        if (km > 80)  return "rural";
        if (km > 30)  return "mixta";
        return "urbana";
    }

    // Infiere el tipo de vehículo desde la capacidad del camión.
    // Camion no tiene campo tipo, así que usamos capacidadCargaKg.

    private String inferirTipoVehiculo(Camion camion) {
        if (camion == null || camion.getCapacidadCargaKg() == null) return "camion_mediano";
        int cap = camion.getCapacidadCargaKg();
        if (cap <= 5000)  return "camion_pequeno";
        if (cap <= 12000) return "camion_mediano";
        if (cap <= 20000) return "camion_grande";
        return "semi_trailer";
    }

    // Hora pico 7–9h y 17–19h
    private boolean esHoraPico(LocalDateTime dt) {
        int hora = dt.getHour();
        return (hora >= 7 && hora <= 9) || (hora >= 17 && hora <= 19);
    }

    // Fin de semana: sábado o domingo
    private boolean esFinDeSemana(LocalDateTime dt) {
        return switch (dt.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> true;
            default -> false;
        };
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        return 0;
    }
}