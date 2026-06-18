package com.logitrack.sistema_logistica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.logitrack.sistema_logistica.dto.IncidenciaMapaDTO;
import com.logitrack.sistema_logistica.model.Incidencia;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Integer> {

    List<Incidencia> findAllByOrderByFechaReporteDesc();

    // Método para contar incidencias en un rango de fechas para el reporte de eficiencia
    long countByFechaReporteBetween(LocalDateTime inicio, LocalDateTime fin);

    /**
     * TAREA #528: Proyección optimizada para el mapa histórico.
     * Solo extrae las columnas necesarias sin cargar entidades completas en memoria.
     */
    @Query("SELECT new com.logitrack.sistema_logistica.dto.IncidenciaMapaDTO(" +
           "i.latitud, i.longitud, CAST(i.estado AS string), c.patente, " +
           "CONCAT(p.nombre, ' ', p.apellido), i.descripcion, i.fechaReporte) " +
           "FROM Incidencia i " +
           "LEFT JOIN i.envio e " +
           "LEFT JOIN e.camion c " +
           "LEFT JOIN e.chofer ch " +
           "LEFT JOIN ch.personaAsociada p " +
           "WHERE i.latitud IS NOT NULL AND i.longitud IS NOT NULL")
    List<IncidenciaMapaDTO> obtenerIncidenciasOptimizadasParaMapa();
}