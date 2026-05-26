package com.logitrack.sistema_logistica.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logitrack.sistema_logistica.model.EmpresaCliente;
import com.logitrack.sistema_logistica.repository.EmpresaClienteRepository;

@Service
public class EmpresaClienteService {
    @Autowired
    private EmpresaClienteRepository empresaClienteRepository;

    @Transactional
    public EmpresaCliente crearEmpresa(EmpresaCliente nuevaEmpresa) {
        
        // Validación básica de integridad
        if (nuevaEmpresa.getCuit() == null || nuevaEmpresa.getCuit().isBlank()) {
            throw new IllegalArgumentException("El CUIT es obligatorio para registrar una nueva empresa.");
        }

        // Validación de CUIT duplicado (Bug #338 / Test #307)
        if (empresaClienteRepository.existsByCuit(nuevaEmpresa.getCuit())) {
            throw new RuntimeException("Validación fallida: Ya existe una empresa registrada con el CUIT " + nuevaEmpresa.getCuit());
        }

        // Si pasa las validaciones, se guarda en PostgreSQL
        return empresaClienteRepository.save(nuevaEmpresa);
    }
}
