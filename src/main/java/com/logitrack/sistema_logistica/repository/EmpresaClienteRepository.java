package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.EmpresaCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaClienteRepository extends JpaRepository<EmpresaCliente, String> {
}