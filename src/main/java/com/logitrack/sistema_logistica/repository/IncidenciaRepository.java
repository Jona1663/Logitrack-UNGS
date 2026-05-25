package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.Incidencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Integer> {

    List<Incidencia> findAllByOrderByFechaReporteDesc();
}