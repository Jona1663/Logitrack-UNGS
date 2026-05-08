package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.Empresa_Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Empresa_ClienteRepository extends JpaRepository<Empresa_Cliente, String> {
}