package com.logitrack.sistema_logistica.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.logitrack.sistema_logistica.model.EmpresaCliente;
import com.logitrack.sistema_logistica.service.EmpresaClienteService;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaClienteController {

    @Autowired
    private EmpresaClienteService empresaClienteService;

    @PostMapping
    public ResponseEntity<?> nuevaEmpresa(@RequestBody EmpresaCliente empresa) {
       try {
            EmpresaCliente nuevaEmpresa = empresaClienteService.crearEmpresa(empresa);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaEmpresa);
        } catch (RuntimeException e) {
            
            // Atajamos tu validación y devolvemos un 400 Bad Request
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
