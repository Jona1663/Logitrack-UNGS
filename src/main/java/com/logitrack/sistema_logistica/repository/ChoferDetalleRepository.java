package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.ChoferDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChoferDetalleRepository extends JpaRepository<ChoferDetalle, Integer> {
}