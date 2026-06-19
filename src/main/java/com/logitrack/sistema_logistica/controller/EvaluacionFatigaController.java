package com.logitrack.sistema_logistica.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logitrack.sistema_logistica.dto.EvaluacionFatigaRequestDTO;
import com.logitrack.sistema_logistica.dto.EvaluacionFatigaResponseDTO;
import com.logitrack.sistema_logistica.service.EvaluacionFatigaService;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluaciones")
public class EvaluacionFatigaController {
    @Autowired 
    private EvaluacionFatigaService service;

    @PostMapping
    public ResponseEntity<EvaluacionFatigaResponseDTO> registrarEvaluacion(
            @RequestBody EvaluacionFatigaRequestDTO dto, 
            Authentication auth) {
        return ResponseEntity.ok(service.procesarEvaluacion(dto, auth.getName()));
    }

    @PostMapping("/{id}/resetear")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> resetear(@PathVariable Long id) {
        service.resetearEvaluacion(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/autorizar-fuerza-mayor")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> autorizar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String motivo = body.get("motivo");
        // Pasamos también el username para cumplir con "guarda el ID/usuario del supervisor"
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        service.autorizarForzado(id, motivo, username);
        return ResponseEntity.ok().build();
    }    

}
