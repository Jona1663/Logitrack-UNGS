package com.logitrack.sistema_logistica.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api") // Mapea a la base de tu API
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        // Retorna un HTTP 200 OK instantáneo
        return ResponseEntity.ok("OK");
    }
}