package com.logitrack.sistema_logistica.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logitrack.sistema_logistica.model.AlertaWeb;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;
import com.logitrack.sistema_logistica.service.AlertaWebService;

@RestController
@RequestMapping("/api/alertas-web")
public class AlertaWebController {

    @Autowired
    private AlertaWebService alertaWebService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping(value = "/pendientes", produces = "application/json")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<List<AlertaWeb>> obtenerAlertasPendientes(Principal principal) {

        Usuario usuario = usuarioRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<AlertaWeb> pendientes = alertaWebService.obtenerPendientes(usuario.getIdUsuario());
        return ResponseEntity.ok(pendientes);
    }

    @PatchMapping("/{id}/leer")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<?> marcarAlertaComoLeida(@PathVariable Integer id) {

        alertaWebService.marcarComoLeida(id);
        return ResponseEntity.noContent().build();
    }
}