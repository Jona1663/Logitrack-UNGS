package com.logitrack.sistema_logistica.service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClimaService {

    @Value("${openweather.api.key}")
    private String apiKey;

    @Value("${openweather.api.url:https://api.openweathermap.org/data/2.5/weather}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String obtenerCondicionClimatica(Double lat, Double lon) {
        if (lat == null || lon == null) {
            log.warn("[Clima] Coordenadas nulas, usando default 'despejado'");
            return "despejado";
        }

        try {
            String url = apiUrl + "?lat=" + lat + "&lon=" + lon
                    + "&appid=" + apiKey + "&units=metric";

            Map response = restTemplate.getForObject(url, Map.class);

            // OpenWeatherMap devuelve weather[0].main con la condición principal
            List<Map<?, ?>> weatherList = (List<Map<?, ?>>) response.get("weather");
            if (weatherList == null || weatherList.isEmpty())
                return "despejado";

            String condicion = String.valueOf(weatherList.get(0).get("main")).toLowerCase();
            String resultado = mapearCondicion(condicion);

            log.info("[Clima] Coordenadas ({},{}) → OWM:{} → ML:{}", lat, lon, condicion, resultado);
            return resultado;

        } catch (Exception e) {
            log.warn("[Clima] OpenWeatherMap no disponible: {}. Usando default 'despejado'", e.getMessage());
            return "despejado";
        }
    }

    // Mapea las condiciones de OpenWeatherMap a los valores que acepta el modelo ML
    private String mapearCondicion(String owmCondicion) {
        return switch (owmCondicion) {
            case "drizzle" -> "lluvia_leve";
            case "rain" -> "lluvia_leve";
            case "thunderstorm" -> "lluvia_fuerte";
            case "snow" -> "lluvia_fuerte"; // nieve → impacto similar
            case "fog", "mist", "haze" -> "niebla";
            case "squall", "tornado", "ash" -> "granizo"; // condiciones extremas
            default -> "despejado"; // "clear", "clouds", etc.
        };
    }
}