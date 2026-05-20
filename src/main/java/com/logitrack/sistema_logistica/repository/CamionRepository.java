package com.logitrack.sistema_logistica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;

@Repository
public interface CamionRepository extends JpaRepository<Camion, String> {

    @Query("""
    SELECT c FROM Camion c
    WHERE c.patente NOT IN (
        SELECT e.camion.patente FROM Envio e
        WHERE e.estadoActual IN :estadosActivos
        AND e.camion IS NOT NULL
    )
""")
List<Camion> findCamionesDisponibles(
    @Param("estadosActivos") List<EstadoEnvio> estadosActivos
);
}