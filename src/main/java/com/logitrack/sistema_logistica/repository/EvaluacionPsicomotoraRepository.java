package com.logitrack.sistema_logistica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.logitrack.sistema_logistica.model.EvaluacionPsicomotora;
import com.logitrack.sistema_logistica.model.enums.EstadoEvaluacionEnum;

@Repository
public interface EvaluacionPsicomotoraRepository extends JpaRepository<EvaluacionPsicomotora, Long> {
    // Método para la Tarea #601: Verifica si existe un rechazo activo para un envío específico
    // Spring Data JPA crea la query solo por el nombre del método
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EvaluacionPsicomotora e WHERE e.idEnvio.idEnvio = :idEnvio AND e.estadoBloqueo = :estadoBloqueo")
    boolean existsByEnvioIdEnvioAndEstadoBloqueo(@Param("idEnvio") String idEnvio, @Param("estadoBloqueo") EstadoEvaluacionEnum estadoBloqueo);
}
