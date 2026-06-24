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
import com.logitrack.sistema_logistica.model.Camion;
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

    // US-45 Servicios para ML 
    // Se inyectan los clientes para obtener datos de clima, tráfico y el microservicio de ML.
    private final ETAClientService etaClientService;
    private final ClimaService climaService;
    private final TraficoService traficoService;

    private static final double VELOCIDAD_PROMEDIO_KMH = 65.0;

    /**
     * Se comunica con GraphHopper, obtiene la ruta y la guarda en la Base de Datos.
     */
    public void generarYGuardarRuta(Envio envio) {
    //Version 1
    /*
        Double latInicio;
        Double lonInicio;
        Double latFin;
        Double lonFin;

        if (envio.getEstadoActual() == EstadoEnvio.EN_TRANSITO) {
            // TRAMO 1: UNGS -> Origen
            latInicio = -34.522881;
            lonInicio = -58.700085;
            latFin = envio.getOrigen().getLatitud();
            lonFin = envio.getOrigen().getLongitud();
        } else {
            // TRAMO 2: Origen -> Destino
            if (envio.getRutaEnvio() != null) {
                rutaEnvioRepository.delete(envio.getRutaEnvio());
                rutaEnvioRepository.flush();
                envio.setRutaEnvio(null);
            }
            latInicio = envio.getOrigen().getLatitud();
            lonInicio = envio.getOrigen().getLongitud();
            latFin = envio.getDestino().getLatitud();
            lonFin = envio.getDestino().getLongitud();
        }
    */

        Double latInicio;
        Double lonInicio;
        Double latFin; 
        Double lonFin; 

        // Lógica mejorada para definir el punto de partida
        if (envio.getEstadoActual() == EstadoEnvio.EN_TRANSITO) {
            // Viaje de aproximación desde la base
            latInicio = -34.522881; 
            lonInicio = -58.700085;
            latFin = envio.getOrigen().getLatitud();
            lonFin = envio.getOrigen().getLongitud();
        } else if(envio.getEstadoActual() == EstadoEnvio.EN_REPARTO){
            // En cualquier otro caso (EN_PUNTO_DE_RECOLECCION o EN_REPARTO), 
            // el origen del movimiento es el establecimiento de origen
            latInicio = envio.getOrigen().getLatitud();
        lonInicio = envio.getOrigen().getLongitud();
        latFin = envio.getDestino().getLatitud();
        lonFin = envio.getDestino().getLongitud();
        }else{
            // Estado PENDIENTE o casos por defecto: Mantener en UNGS
            latInicio = -34.522881;
            lonInicio = -58.700085;
            latFin = -34.522881;
            lonFin = -58.700085;
        }

        JsonNode pathData = graphHopperService.obtenerRuta(latInicio, lonInicio, latFin, lonFin);

        Double distanciaKm = pathData.path("distance").asDouble() / 1000.0;
        Long tiempoSegundos = pathData.path("time").asLong() / 1000L;
        String polylineJson = pathData.path("points").path("coordinates").toString();

        RutaEnvio ruta = rutaEnvioRepository.findByEnvio(envio).orElse(null);

        if (ruta == null) {
            ruta = RutaEnvio.builder()
                    .envio(envio)
                    .polylineJson(polylineJson)
                    .distanciaTotalKm(distanciaKm)
                    .duracionTotalSegundos(tiempoSegundos)
                    .build();
        } else {
            ruta.setPolylineJson(polylineJson);
            ruta.setDistanciaTotalKm(distanciaKm);
            ruta.setDuracionTotalSegundos(tiempoSegundos);
        }
        rutaEnvioRepository.save(ruta);

        envio.setRutaEnvio(ruta);
        envio.setFechaSalida(LocalDateTime.now());

        // Antes: envio.setFechaEstimadaLlegada(LocalDateTime.now().plusSeconds(tiempoSegundos));
        // Ahora: la fecha estimada se asigna desde EnvioService usando el modelo ML
    }

    public LocalDateTime calcularETAConML(Envio envio, Camion camion) {
        LocalDateTime fechaSalida = LocalDateTime.now();

        // Obtener condiciones actuales (clima y tráfico) desde las coordenadas del origen
        String clima = climaService.obtenerCondicionClimatica(
                envio.getOrigen().getLatitud(),
                envio.getOrigen().getLongitud()
        );
        String trafico = traficoService.obtenerNivelTrafico(
                envio.getOrigen().getLatitud(),
                envio.getOrigen().getLongitud()
        );

        // Llamar al microservicio de ML
        ETAClientService.ETAResult result = etaClientService.calcularEta(envio, camion, clima, trafico);

        if (result.disponible) {
            return fechaSalida.plusMinutes(result.etaMinutos);
        }

        // microservicio caído -> usar velocidad fija 65 km/h
        return calcularETA(envio.getDistanciaKm(), fechaSalida);
    }

    //Version2
    // 1. Método original (sin parámetros), mantiene compatibilidad con el resto del sistema
    public Map<String, Object> calcularUbicacionInterpolada(Envio envio) {
        return calcularUbicacionInterpolada(envio, -1.0); // -1 indica "modo tiempo real"
    }

    // 2. Nuevo método para tu TrackingPublicoService
    public Map<String, Object> calcularUbicacionMitad(Envio envio) {
        return calcularUbicacionInterpolada(envio, 0.5); // 0.5 indica "fijo en la mitad"
    }

    // 3. Lógica centralizada (El "motor" que hace el trabajo pesado)
    private Map<String, Object> calcularUbicacionInterpolada(Envio envio, double porcentajeForzado) {
        if (envio.getEstadoActual() != EstadoEnvio.EN_TRANSITO && envio.getEstadoActual() != EstadoEnvio.EN_REPARTO) {
            throw new RuntimeException("El envío no se encuentra en un estado activo de transporte.");
        }

        RutaEnvio ruta = envio.getRutaEnvio();
        if (ruta == null || ruta.getPolylineJson() == null || ruta.getPolylineJson().isBlank()) {
            throw new RuntimeException("No se encontraron datos de ruta registrados para este envío.");
        }

        try {
            double porcentaje;
            
            // Lógica inteligente: ¿tiempo real o fijo?
            if (porcentajeForzado >= 0) {
                porcentaje = porcentajeForzado;
            } else {
                LocalDateTime ahora = LocalDateTime.now();
                LocalDateTime salida = envio.getFechaSalida();
                long segundosTranscurridos = Duration.between(salida, ahora).getSeconds();
                long duracionTotal = ruta.getDuracionTotalSegundos();
                
                porcentaje = (double) segundosTranscurridos / duracionTotal;
                porcentaje = Math.min(Math.max(porcentaje, 0.0), 1.0); // Clamp entre 0 y 1
            }

            // Conversión de coordenadas (La lógica que ya tenías)
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
            response.put("latitudActual", puntoActual.y);
            response.put("longitudActual", puntoActual.x);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error en el procesamiento geométrico: " + e.getMessage());
        }
    }    





    // Calcula la ubicación exacta en tiempo real simulado (interpolación sobre el LineString)
    /* Metodo original
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

            if (segundosTranscurridos > duracionTotal) {
                segundosTranscurridos = duracionTotal;
            }

            double porcentaje = (double) segundosTranscurridos / duracionTotal;

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
            response.put("latitudActual", puntoActual.y);
            response.put("longitudActual", puntoActual.x);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error en el procesamiento geométrico del tracking: " + e.getMessage());
        }
    }
    */

    // Parsea la ruta a un formato apto para devolver al frontend
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