package com.logitrack.sistema_logistica.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logitrack.sistema_logistica.dto.ReporteEstadoDTO;
import com.logitrack.sistema_logistica.dto.ReporteGranoDTO;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, String>, JpaSpecificationExecutor<Envio> {
    
        // Forzamos a Hibernate a buscar por el nombre exacto de la variable en el modelo
        @Query("SELECT e FROM Envio e WHERE e.idEnvio = :idEnvio")
        Optional<Envio> buscarPorId(@Param("idEnvio") String idEnvio);
        
        // Devuelve envíos PENDIENTES que no tienen chofer NI camión asignado todavía
         @Query("SELECT e FROM Envio e WHERE e.camion IS NULL AND e.chofer IS NULL " +
        "AND e.estadoActual = com.logitrack.sistema_logistica.model.enums.EstadoEnvio.PENDIENTE")
        List<Envio> findEnviosSinAsignar();

        // Suma simple de kilos para el reporte
        @Query(value = "SELECT COALESCE(SUM(COALESCE(kg_destino, kg_origen)), 0) FROM envios", nativeQuery = true)
        Long sumKilos();

        //consulta personalizada: navegar por las relaciones (desde el Envío hasta el Username del usuario).
        @Query("SELECT e FROM Envio e WHERE e.chofer.personaAsociada.idUsuario.username = :username" + 
                " AND e.estadoActual NOT IN (" +
                " com.logitrack.sistema_logistica.model.enums.EstadoEnvio.ENTREGADO, " +
                " com.logitrack.sistema_logistica.model.enums.EstadoEnvio.CANCELADO)")
        List<Envio> findByChoferUsername(@Param("username") String username);

        // Validación de disponibilidad concurrente
        boolean existsByChoferAndEstadoActualIn(
        com.logitrack.sistema_logistica.model.ChoferDetalle chofer,
        List<EstadoEnvio> estados
        );

        boolean existsByCamionAndEstadoActualIn(
        com.logitrack.sistema_logistica.model.Camion camion,
        List<EstadoEnvio> estados);

        // CONSULTAS PARA REPORTES (DASHBOARD)
        @Query("SELECT new com.logitrack.sistema_logistica.dto.ReporteEstadoDTO(" +
           "CAST(e.estadoActual as string), " +
           "COUNT(e), " +
           "COALESCE(SUM(COALESCE(e.kgDestino, e.kgOrigen)), 0L)) " +
           "FROM Envio e GROUP BY e.estadoActual")
        List<ReporteEstadoDTO> obtenerMetricasPorEstado();

        // CONSULTAS PARA REPORTES CON FILTRO DE FECHAS
        @Query("SELECT COUNT(e) FROM Envio e WHERE e.fechaCreacion >= :inicio AND e.fechaCreacion <= :fin")
        long countEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

        @Query("SELECT COALESCE(SUM(COALESCE(e.kgDestino, e.kgOrigen)), 0L) FROM Envio e WHERE e.fechaCreacion >= :inicio AND e.fechaCreacion <= :fin")
        Long sumKilosEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

        @Query("SELECT new com.logitrack.sistema_logistica.dto.ReporteEstadoDTO(" +
           "CAST(e.estadoActual as string), " +
           "COUNT(e), " +
           "COALESCE(SUM(COALESCE(e.kgDestino, e.kgOrigen)), 0L)) " +
           "FROM Envio e WHERE e.fechaCreacion >= :inicio AND e.fechaCreacion <= :fin GROUP BY e.estadoActual")
        List<ReporteEstadoDTO> obtenerMetricasPorEstadoEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin); 

        // Cantidad de envíos y kilos por tipo de grano
        @Query("SELECT new com.logitrack.sistema_logistica.dto.ReporteGranoDTO(" +
                "CAST(e.tipoGrano as string), COUNT(e), COALESCE(SUM(e.kgOrigen), 0L)) " +
                 "FROM Envio e WHERE e.fechaCreacion BETWEEN :inicio AND :fin " +
                "GROUP BY e.tipoGrano")
        List<ReporteGranoDTO> obtenerMetricasPorGrano(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

        // Cuenta la cantidad de envíos que llegaron a tiempo
        @Query("SELECT COUNT(e) FROM Envio e WHERE e.fechaLlegada IS NOT NULL AND e.fechaLlegada <= e.fechaEstimadaLlegada AND e.fechaCreacion BETWEEN :inicio AND :fin")
        long countEnviosATiempoEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

        // Suma los kilos de los envíos que llegaron a tiempo
        @Query("SELECT COALESCE(SUM(e.kgOrigen), 0L) FROM Envio e WHERE e.fechaLlegada IS NOT NULL AND e.fechaLlegada <= e.fechaEstimadaLlegada AND e.fechaCreacion BETWEEN :inicio AND :fin")
        long sumKilosATiempoEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

        //Filtra estrictamente por estado 'ENTREGADO' y se asegura de que ninguna fecha sea nula
        @Query("SELECT e FROM Envio e " +
       "WHERE e.estadoActual = 'ENTREGADO' " +
       "AND e.fechaLlegada IS NOT NULL " +
       "AND e.fechaEstimadaLlegada IS NOT NULL " +
       "AND e.fechaCreacion BETWEEN :inicio AND :fin")
        List<Envio> obtenerEnviosCompletadosParaCumplimiento(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin);

        @Query("SELECT e FROM Envio e WHERE e.fechaCreacion >= :inicio AND e.fechaCreacion <= :fin ORDER BY e.fechaCreacion ASC")
        Stream<Envio> obtenerEnviosComoStreamParaExportacion(
                @Param("inicio") LocalDateTime inicio, 
                @Param("fin") LocalDateTime fin
        );

        // Arregla el bug de la Prueba 3 (Cuenta SOLO los completados)
        @Query("SELECT COUNT(e) FROM Envio e WHERE e.estadoActual = 'ENTREGADO' AND e.fechaLlegada IS NOT NULL AND e.fechaEstimadaLlegada IS NOT NULL AND e.fechaCreacion BETWEEN :inicio AND :fin")
        long countCompletadosEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

        // Stream exclusivo para Cumplimiento (Solo entregados)
        @Query("SELECT e FROM Envio e WHERE e.estadoActual = 'ENTREGADO' AND e.fechaLlegada IS NOT NULL AND e.fechaEstimadaLlegada IS NOT NULL AND e.fechaCreacion BETWEEN :inicio AND :fin ORDER BY e.fechaCreacion ASC")
        Stream<Envio> obtenerEnviosCompletadosComoStream(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

        // Stream para Histórico completo (Usado en el reporte de Estados sin filtro)
        @Query("SELECT e FROM Envio e ORDER BY e.fechaCreacion ASC")
        Stream<Envio> obtenerTodosComoStream();

        // Stream para envíos A Tiempo
        @Query("SELECT e FROM Envio e WHERE e.fechaLlegada IS NOT NULL AND e.fechaLlegada <= e.fechaEstimadaLlegada AND e.fechaCreacion BETWEEN :inicio AND :fin ORDER BY e.fechaCreacion ASC")
        Stream<Envio> obtenerEnviosATiempoComoStream(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

        // Trae todos los envíos creados entre dos fechas para el reporte detallado (CSV y Excel)
        List<Envio> findByFechaCreacionBetween(LocalDateTime startDate, LocalDateTime endDate);

        @Query("SELECT COUNT(e) FROM Envio e WHERE e.estadoActual = 'ENTREGADO' AND e.fechaCreacion >= :fechaInicio AND e.fechaCreacion <= :fechaFin")
        long countTotalEntregados(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

        @Query("SELECT COUNT(e) FROM Envio e WHERE e.estadoActual = 'ENTREGADO' AND e.fechaLlegada <= e.fechaEstimadaLlegada AND e.fechaCreacion >= :fechaInicio AND e.fechaCreacion <= :fechaFin")
        long countEntregadosATiempo(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

        @Query("SELECT COUNT(e) FROM Envio e WHERE e.estadoActual = 'ENTREGADO' AND e.fechaLlegada > e.fechaEstimadaLlegada AND e.fechaCreacion >= :fechaInicio AND e.fechaCreacion <= :fechaFin")
        long countConRetraso(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

}