package com.logitrack.sistema_logistica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logitrack.sistema_logistica.model.ChoferDetalle;

@Repository
public interface ChoferDetalleRepository extends JpaRepository<ChoferDetalle, Integer> {
    List<ChoferDetalle> findByDisponibleTrue();
}