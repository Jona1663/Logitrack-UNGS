package com.logitrack.sistema_logistica.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.logitrack.sistema_logistica.model.EvaluacionPsicomotora;
import com.logitrack.sistema_logistica.model.enums.EstadoEvaluacionEnum;

@Repository
public interface EvaluacionPsicomotoraRepository extends JpaRepository<EvaluacionPsicomotora, Long> {
    // Verifica si existe un rechazo activo para un envío
    // específico
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EvaluacionPsicomotora e WHERE e.idEnvio.idEnvio = :idEnvio AND e.estadoBloqueo = :estadoBloqueo")
    boolean existsByEnvioIdEnvioAndEstadoBloqueo(@Param("idEnvio") String idEnvio,
            @Param("estadoBloqueo") EstadoEvaluacionEnum estadoBloqueo);

    @Query("SELECT e FROM EvaluacionPsicomotora e WHERE e.choferId.idChofer = :choferId AND e.idEnvio.idEnvio = :envioId AND e.estadoBloqueo IN :estados")
    List<EvaluacionPsicomotora> buscarEvaluacionesParaDesvincular(
            @Param("choferId") Integer choferId,
            @Param("envioId") String envioId,
            @Param("estados") List<EstadoEvaluacionEnum> estados);

    // Busca la última evaluación de un viaje que tenga un estado específico (ej.
    // RECHAZADO)
    Optional<EvaluacionPsicomotora> findFirstByIdEnvio_IdEnvioAndEstadoBloqueoOrderByFechaCreacionDesc(String idEnvio,
            EstadoEvaluacionEnum estadoBloqueo);
}
