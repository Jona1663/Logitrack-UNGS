package com.logitrack.sistema_logistica.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.logitrack.sistema_logistica.dto.AlertaListadoDTO;
import com.logitrack.sistema_logistica.dto.IncidenciaDTO;
import com.logitrack.sistema_logistica.dto.IncidenciaMapaDTO;
import com.logitrack.sistema_logistica.dto.ResolverIncidenciaDTO;
import com.logitrack.sistema_logistica.service.IncidenciaService;
import com.logitrack.sistema_logistica.service.IncidenciaMapaService;


@RestController
@RequestMapping("/api")
public class IncidenciaController {

    @Autowired
    private IncidenciaMapaService incidenciaMapaService;

    @Autowired
    private IncidenciaService incidenciaService;

    @PostMapping("/envios/{idEnvio}/incidencias")
    @PreAuthorize("hasRole('CHOFER')")
    public ResponseEntity<?> reportarIncidencia(
            @PathVariable String idEnvio,
            @RequestBody IncidenciaDTO incidenciaDTO) {

        try {
            // Validación básica requerida por el contrato
            if (incidenciaDTO.getTipoIncidencia() == null) {
                return ResponseEntity.badRequest().body("{\"message\": \"El tipo de incidencia es obligatorio.\"}");
            }

            incidenciaService.reportarIncidencia(idEnvio, incidenciaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).build(); // 201 Created sin body

        } catch (IllegalStateException e) {
            // 409 Conflict: El viaje no está EN_TRANSITO o EN_REPARTO
            return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"message\": \"" + e.getMessage() + "\"}");

        } catch (RuntimeException e) {
            // 404 Not Found: El viaje no existe
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"Envío no encontrado.\"}");
            }
            // 400 Bad Request para otros errores
            return ResponseEntity.badRequest().body("{\"message\": \"" + e.getMessage() + "\"}");
        }
    }

    // US 33 ENDPOINTS DEL SUPERVISOR

    // Listado de Alertas
    @GetMapping("/incidencias/alertas")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<List<AlertaListadoDTO>> obtenerAlertas() {
        List<AlertaListadoDTO> alertas = incidenciaService.listarAlertas();
        return ResponseEntity.ok(alertas); // 200 OK
    }

    // Resolución de Alertas
    @PatchMapping("/incidencias/{id}/resolver")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> resolverIncidencia(
            @PathVariable Integer id,
            @RequestBody(required = false) ResolverIncidenciaDTO dto) {

        try {
            incidenciaService.resolverIncidencia(id, dto);
            return ResponseEntity.noContent().build(); // 204 No Content (Éxito silencioso, como pidió el Front)

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"Incidencia no encontrada.\"}");
        }
    }

    // US 63 ENDPOINT PARA EL MAPA DE INCIDENCIAS (SUPERVISOR)
    @GetMapping("/incidencias/mapa")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<List<IncidenciaMapaDTO>> obtenerMapaIncidencias() {
        List<IncidenciaMapaDTO> incidencias = incidenciaMapaService.obtenerDatosMapaHistorico();
        return ResponseEntity.ok(incidencias); // 200 OK
    }

}