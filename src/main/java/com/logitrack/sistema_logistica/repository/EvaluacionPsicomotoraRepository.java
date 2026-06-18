package com.logitrack.sistema_logistica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.logitrack.sistema_logistica.model.EvaluacionPsicomotora;
import com.logitrack.sistema_logistica.model.enums.EstadoEvaluacionEnum;

@Repository
public interface EvaluacionPsicomotoraRepository extends JpaRepository<EvaluacionPsicomotora, Long> {
    // Método para la Tarea #601: Verifica si existe un rechazo activo para un envío específico
    // Spring Data JPA crea la query solo por el nombre del método
    boolean existsByEnvioIdEnvioAndEstadoBloqueo(String idEnvio, EstadoEvaluacionEnum estadoBloqueo);
}
