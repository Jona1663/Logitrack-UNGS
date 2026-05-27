package com.logitrack.sistema_logistica.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.RutaEnvio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.repository.RutaEnvioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrackingGeospatialService {

    private final GraphHopperService graphHopperService;
    private final RutaEnvioRepository rutaEnvioRepository;

    private static final double VELOCIDAD_PROMEDIO_KMH = 65.0;

    /**
     * Se comunica con GraphHopper, obtiene la ruta y la guarda en la Base de Datos.
     */
    public void generarYGuardarRuta(Envio envio) {
        Double latInicio;
        Double lonInicio;
        Double latFin;
        Double lonFin;

        if (envio.getEstadoActual() == EstadoEnvio.EN_TRANSITO) {
            // TRAMO 1: UNGS -> Origen
            latInicio = -34.522881; // Coordenada UNGS
            lonInicio = -58.700085; // Coordenada UNGS
            latFin = envio.getOrigen().getLatitud();
            lonFin = envio.getOrigen().getLongitud();
        } else {
            // TRAMO 2: Origen -> Destino (Yendo a entregar)
            if (envio.getRutaEnvio() != null) {
                // 1. La borramos de la base de datos
                rutaEnvioRepository.delete(envio.getRutaEnvio());
                rutaEnvioRepository.flush();
                // 2. La desvinculamos del objeto en memoria para que JPA no intente volver a guardarla
                envio.setRutaEnvio(null);                 
            }
            latInicio = envio.getOrigen().getLatitud();
            lonInicio = envio.getOrigen().getLongitud();
            latFin = envio.getDestino().getLatitud();
            lonFin = envio.getDestino().getLongitud();
        }

        // 1. Le pedimos el JSON a GraphHopper con las coordenadas
        JsonNode pathData = graphHopperService.obtenerRuta(latInicio, lonInicio, latFin, lonFin);

// 2. Extraemos la info
        Double distanciaKm = pathData.path("distance").asDouble() / 1000.0;
        Long tiempoSegundos = pathData.path("time").asLong() / 1000L;
        String polylineJson = pathData.path("points").path("coordinates").toString();

        RutaEnvio ruta = rutaEnvioRepository.findByEnvio(envio).orElse(null);

        if (ruta == null) {
            // Si el envío no tiene ruta (Ej: primer tramo), creamos una nueva
            ruta = RutaEnvio.builder()
                    .envio(envio)
                    .polylineJson(polylineJson)
                    .distanciaTotalKm(distanciaKm)
                    .duracionTotalSegundos(tiempoSegundos)
                    .build();
        } else {
            // Si el envío YA tiene ruta (Ej: segundo tramo), le pisamos los datos (UPDATE)
            ruta.setPolylineJson(polylineJson);
            ruta.setDistanciaTotalKm(distanciaKm);
            ruta.setDuracionTotalSegundos(tiempoSegundos);
        }
        rutaEnvioRepository.save(ruta);

        // 4. Actualizamos el objeto Envio en memoria
        envio.setRutaEnvio(ruta);
        envio.setFechaSalida(LocalDateTime.now());
        envio.setFechaEstimadaLlegada(LocalDateTime.now().plusSeconds(tiempoSegundos));
    }

    /**
     * Calcula la ubicación exacta en tiempo real simulado (interpolación sobre el LineString).
     */
    public Map<String, Object> calcularUbicacionInterpolada(Envio envio) {
        if (envio.getEstadoActual() != EstadoEnvio.EN_TRANSITO
                && envio.getEstadoActual() != EstadoEnvio.EN_REPARTO) {
            throw new RuntimeException("El envío no se encuentra en un estado activo de transporte.");
        }

        RutaEnvio ruta = envio.getRutaEnvio();
        if (ruta == null || ruta.getPolylineJson() == null || ruta.getPolylineJson().isBlank()) {
            throw new RuntimeException("No se encontraron datos de ruta registrados para este envío.");
        }

        try {
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime salida = envio.getFechaSalida();
            Long duracionTotal = ruta.getDuracionTotalSegundos();

            long segundosTranscurridos = Duration.between(salida, ahora).getSeconds();

            // Evitamos que el porcentaje supere el 100%
            if (segundosTranscurridos > duracionTotal) {
                segundosTranscurridos = duracionTotal;
            }

            double porcentaje = (double) segundosTranscurridos / duracionTotal;

            // Procesamiento espacial JTS
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arrayCoordenadas = mapper.readTree(ruta.getPolylineJson());

            Coordinate[] coords = new Coordinate[arrayCoordenadas.size()];
            for (int i = 0; i < arrayCoordenadas.size(); i++) {
                JsonNode punto = arrayCoordenadas.get(i);
                coords[i] = new Coordinate(punto.get(0).asDouble(), punto.get(1).asDouble());
            }

            GeometryFactory gf = new GeometryFactory();
            LineString lineaRuta = gf.createLineString(coords);

            LengthIndexedLine indexedLine = new LengthIndexedLine(lineaRuta);
            double distanciaObjetivo = lineaRuta.getLength() * porcentaje;
            Coordinate puntoActual = indexedLine.extractPoint(distanciaObjetivo);

            Map<String, Object> response = new HashMap<>();
            response.put("idEnvio", envio.getIdEnvio());
            response.put("estadoActual", envio.getEstadoActual().name());
            response.put("porcentajeCompletado", porcentaje * 100.0);
            response.put("latitudActual", puntoActual.y); // Y es Latitud en JTS para nuestro caso
            response.put("longitudActual", puntoActual.x); // X es Longitud

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error en el procesamiento geométrico del tracking: " + e.getMessage());
        }
    }

    /**
     * Parsea la ruta a un formato apto para devolver al frontend.
     */
    public JsonNode extraerGeometriaRuta(RutaEnvio ruta) {
        if (ruta == null || ruta.getPolylineJson() == null || ruta.getPolylineJson().isBlank()) {
            throw new RuntimeException("El envío no tiene una ruta generada aún.");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(ruta.getPolylineJson());
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar los datos geográficos de la ruta.");
        }
    }

    /**
     * Cálculo matemático puro de ETA basado en distancia.
     */
    public LocalDateTime calcularETA(Double distanciaKm, LocalDateTime fechaSalida) {
        if (distanciaKm == null || distanciaKm <= 0 || fechaSalida == null) {
            return null;
        }
        long minutosViaje = Math.round((distanciaKm / VELOCIDAD_PROMEDIO_KMH) * 60);
        return fechaSalida.plusMinutes(minutosViaje);
    }
}