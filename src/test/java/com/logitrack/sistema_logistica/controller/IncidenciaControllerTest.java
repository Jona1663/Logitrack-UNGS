package com.logitrack.sistema_logistica.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.logitrack.sistema_logistica.dto.AlertaListadoDTO;
import com.logitrack.sistema_logistica.dto.IncidenciaDTO;
import com.logitrack.sistema_logistica.dto.ResolverIncidenciaDTO;
import com.logitrack.sistema_logistica.model.enums.TipoIncidencia;
import com.logitrack.sistema_logistica.service.IncidenciaService;

@ExtendWith(MockitoExtension.class)
public class IncidenciaControllerTest {

    @Mock
    private IncidenciaService incidenciaService;

    @InjectMocks
    private IncidenciaController incidenciaController;

    private IncidenciaDTO incidenciaDTO;

    @BeforeEach
    void setUp() {
        incidenciaDTO = new IncidenciaDTO();
        incidenciaDTO.setTipoIncidencia(TipoIncidencia.MECANICA);
        incidenciaDTO.setDescripcion("Problema con el motor");
    }

    // =========================================================================
    // Tests para: POST /api/envios/{idEnvio}/incidencias (Chofer)
    // =========================================================================

    @Test
    void reportarIncidencia_CaminoFeliz_Retorna201() {
        // Act
        ResponseEntity<?> response = incidenciaController.reportarIncidencia("LT-1000", incidenciaDTO);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void reportarIncidencia_SinTipoIncidencia_Retorna400() {
        // Arrange
        incidenciaDTO.setTipoIncidencia(null);

        // Act
        ResponseEntity<?> response = incidenciaController.reportarIncidencia("LT-1000", incidenciaDTO);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("{\"message\": \"El tipo de incidencia es obligatorio.\"}", response.getBody());
    }

    @Test
    void reportarIncidencia_ViajeNoActivo_Retorna409() {
        // Arrange
        doThrow(new IllegalStateException("Solo se pueden reportar incidencias sobre viajes en curso."))
                .when(incidenciaService).reportarIncidencia(eq("LT-1000"), any(IncidenciaDTO.class));

        // Act
        ResponseEntity<?> response = incidenciaController.reportarIncidencia("LT-1000", incidenciaDTO);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("{\"message\": \"Solo se pueden reportar incidencias sobre viajes en curso.\"}", response.getBody());
    }

    @Test
    void reportarIncidencia_EnvioNoExiste_Retorna404() {
        // Arrange
        doThrow(new RuntimeException("Envío no encontrado"))
                .when(incidenciaService).reportarIncidencia(eq("LT-9999"), any(IncidenciaDTO.class));

        // Act
        ResponseEntity<?> response = incidenciaController.reportarIncidencia("LT-9999", incidenciaDTO);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("{\"message\": \"Envío no encontrado.\"}", response.getBody());
    }

    // =========================================================================
    // Tests para: GET /api/incidencias/alertas (Supervisor)
    // =========================================================================

    @Test
    void obtenerAlertas_CaminoFeliz_Retorna200YLista() {
        // Arrange
        AlertaListadoDTO alerta = AlertaListadoDTO.builder()
                .id(1)
                .tipoIncidencia("MECANICA")
                .estado("PENDIENTE")
                .build();

        when(incidenciaService.listarAlertas()).thenReturn(List.of(alerta));

        // Act
        ResponseEntity<List<AlertaListadoDTO>> response = incidenciaController.obtenerAlertas();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("MECANICA", response.getBody().get(0).getTipoIncidencia());
    }

    // =========================================================================
    // Tests para: PATCH /api/incidencias/{id}/resolver (Supervisor)
    // =========================================================================

    @Test
    void resolverIncidencia_CaminoFeliz_Retorna204() {
        // Arrange
        ResolverIncidenciaDTO dto = new ResolverIncidenciaDTO();
        dto.setNotasSupervisor("Resuelto por grúa");

        // Act
        ResponseEntity<?> response = incidenciaController.resolverIncidencia(1, dto);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void resolverIncidencia_IncidenciaNoExiste_Retorna404() {
        // Arrange
        ResolverIncidenciaDTO dto = new ResolverIncidenciaDTO();
        dto.setNotasSupervisor("Resuelto por grúa");

        doThrow(new RuntimeException("Incidencia no encontrada"))
                .when(incidenciaService).resolverIncidencia(eq(99), any(ResolverIncidenciaDTO.class));

        // Act
        ResponseEntity<?> response = incidenciaController.resolverIncidencia(99, dto);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("{\"message\": \"Incidencia no encontrada.\"}", response.getBody());
    }
}