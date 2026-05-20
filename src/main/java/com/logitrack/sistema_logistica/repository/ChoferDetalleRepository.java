package com.logitrack.sistema_logistica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;

@Repository
public interface ChoferDetalleRepository extends JpaRepository<ChoferDetalle, Integer> {

    @Query("""
        SELECT c FROM ChoferDetalle c
        WHERE c.idChofer NOT IN (
            SELECT e.chofer.idChofer FROM Envio e
            WHERE e.estadoActual IN :estadosActivos
            AND e.chofer IS NOT NULL
        )
    """)
    List<ChoferDetalle> findChoferesDisponibles(
        @Param("estadosActivos") List<EstadoEnvio> estadosActivos
    );
}