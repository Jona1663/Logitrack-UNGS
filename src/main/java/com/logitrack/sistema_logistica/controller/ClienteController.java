package com.logitrack.sistema_logistica.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logitrack.sistema_logistica.dto.ClienteRequestDTO;
import com.logitrack.sistema_logistica.model.EmpresaCliente;
import com.logitrack.sistema_logistica.service.ClienteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    //POST /api/clientes Alta de nuevo cliente (US-38). Roles permitidos: OPERADOR y ADMINISTRADOR.
    @PostMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMINISTRADOR')")
    public ResponseEntity<?> crearCliente(@Valid @RequestBody ClienteRequestDTO dto) {
        try {
            EmpresaCliente creado = clienteService.crearCliente(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);

        } catch (IllegalArgumentException ex) {

            // CUIT duplicado → 409 Conflict con mensaje
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", ex.getMessage()));
        }
    }
    
    //GET /api/cliente Listado de clientes (para el catálogo del formulario de envíos).
    @GetMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'SUPERVISOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<EmpresaCliente>> listarClientes() {
        return ResponseEntity.ok(clienteService.listarClientes());
    }
}