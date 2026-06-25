package com.logitrack.sistema_logistica.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logitrack.sistema_logistica.dto.EnvioResumenDTO;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.service.EnvioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chofer")
@RequiredArgsConstructor

public class ChoferController {

    private final EnvioService envioService;

    // Endpoint #113: Retorna la lista de envíos asignados exclusivamente al chofer
    @GetMapping("/envios")
    public ResponseEntity<List<EnvioResumenDTO>> getMisEnvios(Authentication authentication) {

        // El Authentication trae el "username" del JWT automáticamente
        String username = authentication.getName();

        // Obtenemos las entidades del servicio
        List<Envio> misEnvios = envioService.obtenerEnviosPorChofer(username);

        // Las transformamos a DTOs (esto oculta automáticamente la prioridadIa)
        List<EnvioResumenDTO> resumen = misEnvios.stream()
                .map(EnvioResumenDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(resumen);
    }
}