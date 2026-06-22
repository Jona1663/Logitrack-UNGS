package com.logitrack.sistema_logistica.controller;

import com.logitrack.sistema_logistica.dto.TrackingPublicoRequestDTO;
import com.logitrack.sistema_logistica.service.TrackingPublicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/public/tracking")
public class TrackingPublicoController {
    @Autowired
    private TrackingPublicoService service;

    @PostMapping("/consulta")
    public ResponseEntity<?> consultarEnvio(@RequestBody TrackingPublicoRequestDTO request) {
        try {
            return ResponseEntity.ok(service.obtenerInfoPublica(request));
        } catch (Exception e) {
            // El contrato exige este mensaje exacto ante CUALQUIER fallo
            return ResponseEntity.status(400).body(Map.of("error", "No se encontró información para los datos ingresados"));
        }
    }
}
