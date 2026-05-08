package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.Chofer_Detalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Chofer_DetalleRepository extends JpaRepository<Chofer_Detalle, Integer> {
}