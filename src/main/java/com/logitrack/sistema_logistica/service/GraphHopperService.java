package com.logitrack.sistema_logistica.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GraphHopperService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${graphhopper.api.url}")
    private String apiUrl;

    @Value("${graphhopper.api.key}")
    private String apiKey;

    public GraphHopperService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // Llama a GraphHopper y devuelve un JSON Node con los datos de la ruta.

    public JsonNode obtenerRuta(Double latOrigen, Double lonOrigen, Double latDestino, Double lonDestino) {
        // Armamos la URL exacta como la pide GraphHopper
        String url = String.format("%s?point=%s,%s&point=%s,%s&profile=car&points_encoded=false&key=%s",
                apiUrl, latOrigen, lonOrigen, latDestino, lonDestino, apiKey);

        try {
            // Hacemos el GET con RestTemplate
            String response = restTemplate.getForObject(url, String.class);

            // Parseamos la respuesta a un JsonNode para navegarlo fácilmente
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode pathNode = rootNode.path("paths").get(0);

            return pathNode;

        } catch (Exception e) {
            throw new RuntimeException("Error al comunicarse con GraphHopper: " + e.getMessage());
        }
    }
}