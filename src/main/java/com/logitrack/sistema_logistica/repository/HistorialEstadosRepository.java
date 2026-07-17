package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.HistorialEstados;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistorialEstadosRepository extends JpaRepository<HistorialEstados, Integer> {

    List<HistorialEstados> findAllByOrderByFechaHoraDesc();

    @Query("SELECT h FROM HistorialEstados h JOIN FETCH h.envio e JOIN FETCH h.usuario u WHERE e.idEnvio = :idEnvio ORDER BY h.fechaHora DESC")
    List<HistorialEstados> buscarHistorialPorEnvio(@Param("idEnvio") String idEnvio);
}