package com.logitrack.sistema_logistica.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.model.RutaEnvio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.repository.RutaEnvioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrackingGeospatialServiceTest {

    @Mock
    private GraphHopperService graphHopperService;

    @Mock
    private RutaEnvioRepository rutaEnvioRepository;

    @InjectMocks
    private TrackingGeospatialService trackingService;

    @Test
    public void calcularETA_ConValoresValidos_DeberiaRetornarFechaCorrecta() {
        // Arrange
        Double distanciaKm = 130.0; // A 65 km/h, debería tardar exactamente 2 horas
        LocalDateTime fechaSalida = LocalDateTime.of(2026, 5, 22, 10, 0);

        // Act
        LocalDateTime etaCalculado = trackingService.calcularETA(distanciaKm, fechaSalida);

        // Assert
        assertNotNull(etaCalculado);
        assertEquals(LocalDateTime.of(2026, 5, 22, 12, 0), etaCalculado, 
                "El ETA debe ser exactamente 2 horas después de la salida");
    }

    @Test
    public void calcularETA_ConDistanciaNula_DeberiaRetornarNull() {
        // Arrange
        LocalDateTime fechaSalida = LocalDateTime.now();

        // Act
        LocalDateTime etaCalculado = trackingService.calcularETA(null, fechaSalida);

        // Assert
        assertNull(etaCalculado, "Si la distancia es nula, el ETA debe ser nulo");
    }

    @Test
    public void extraerGeometriaRuta_ConRutaInvalida_DeberiaLanzarExcepcion() {
        // Arrange
        RutaEnvio rutaInvalida = RutaEnvio.builder().polylineJson("").build();

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            trackingService.extraerGeometriaRuta(rutaInvalida)
        );
        assertEquals("El envío no tiene una ruta generada aún.", ex.getMessage());
    }

    @Test
    public void extraerGeometriaRuta_ConRutaValida_DeberiaRetornarJsonNode() {
        // Arrange
        String jsonValido = "[[-58.123, -34.456], [-58.124, -34.457]]";
        RutaEnvio rutaValida = RutaEnvio.builder().polylineJson(jsonValido).build();

        // Act
        JsonNode resultado = trackingService.extraerGeometriaRuta(rutaValida);

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isArray(), "El JSON de la ruta debe ser un array de coordenadas");
        assertEquals(2, resultado.size());
    }

@Test
    public void generarYGuardarRuta_DeberiaPersistirNuevaRutaYActualizarEnvio() {
        // Arrange
        Establecimiento origen = new Establecimiento();
        origen.setLatitud(-34.0);
        origen.setLongitud(-58.0);

        Establecimiento destino = new Establecimiento();
        destino.setLatitud(-35.0);
        destino.setLongitud(-59.0);

        Envio envio = Envio.builder()
                .estadoActual(EstadoEnvio.EN_TRANSITO) 
                .origen(origen)
                .destino(destino)
                .build();

        // MOCK CORREGIDO: Ponemos "distance" en la raíz para que el servicio la lea bien
        ObjectNode mockResponse = JsonNodeFactory.instance.objectNode();
        mockResponse.put("distance", 100000.0); // 100.000 metros = 100 km
        mockResponse.put("time", 3600000L);     // 1 hora
        
        // Estructura de puntos dentro del mock
        ObjectNode points = mockResponse.putObject("points");
        ArrayNode coordinates = points.putArray("coordinates");
        
        // Dos puntos para que JTS no falle
        coordinates.addArray().add(-58.0).add(-34.0);
        coordinates.addArray().add(-59.0).add(-35.0);

        when(graphHopperService.obtenerRuta(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(mockResponse);
        
        when(rutaEnvioRepository.save(any(RutaEnvio.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        trackingService.generarYGuardarRuta(envio);

        // Assert
        assertNotNull(envio.getRutaEnvio(), "El envío debe tener una ruta asignada");
        
        // Ahora debería leer los 100.000, dividirlos por 1000 y dar 100.0
        assertEquals(100.0, envio.getRutaEnvio().getDistanciaTotalKm(), 0.01);
        assertEquals(3600L, envio.getRutaEnvio().getDuracionTotalSegundos());
        assertNotNull(envio.getFechaSalida());
        verify(rutaEnvioRepository, times(1)).save(any(RutaEnvio.class));
    }
    @Test
    public void calcularUbicacionInterpolada_DeberiaLanzarExcepcionSiEstadoNoEsActivo() {
        // Arrange
        Envio envioPendiente = Envio.builder()
                .estadoActual(EstadoEnvio.PENDIENTE)
                .build();

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            trackingService.calcularUbicacionInterpolada(envioPendiente)
        );
        assertTrue(ex.getMessage().contains("no se encuentra en un estado activo de transporte"));
    }

    @Test
    public void calcularUbicacionInterpolada_DeberiaCalcularAl100PorCientoSiTiempoExcedio() {
        // Arrange
        long duracionEsperada = 3600L; // 1 hora
        // Simulamos que salió hace 2 horas (excedió el tiempo)
        LocalDateTime fechaSalidaAntigua = LocalDateTime.now().minusSeconds(duracionEsperada + 1800);

        RutaEnvio ruta = RutaEnvio.builder()
                .duracionTotalSegundos(duracionEsperada)
                .polylineJson("[[-58.0, -34.0], [-59.0, -35.0]]")
                .build();

        Envio envio = Envio.builder()
                .idEnvio("LT-100")
                .estadoActual(EstadoEnvio.EN_TRANSITO)
                .fechaSalida(fechaSalidaAntigua)
                .rutaEnvio(ruta)
                .build();

        // Act
        Map<String, Object> ubicacion = trackingService.calcularUbicacionInterpolada(envio);

        // Assert
        assertNotNull(ubicacion);
        assertEquals(100.0, (Double) ubicacion.get("porcentajeCompletado"), "Debería topear al 100%");
        assertEquals(-35.0, (Double) ubicacion.get("latitudActual"), 0.001); // Punto final Y
        assertEquals(-59.0, (Double) ubicacion.get("longitudActual"), 0.001); // Punto final X
    }
}