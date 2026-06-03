package com.logitrack.sistema_logistica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.logitrack.sistema_logistica.model.Incidencia;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Integer> {

    List<Incidencia> findAllByOrderByFechaReporteDesc();

    // Método para contar incidencias en un rango de fechas para el reporte de eficiencia
    long countByFechaReporteBetween(LocalDateTime inicio, LocalDateTime fin);
}