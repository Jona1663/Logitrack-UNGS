package com.logitrack.sistema_logistica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logitrack.sistema_logistica.model.ChoferDetalle;
import java.util.Optional;

@Repository
public interface ChoferDetalleRepository extends JpaRepository<ChoferDetalle, Integer> {
    List<ChoferDetalle> findByDisponibleTrue();

    @Query("SELECT c FROM ChoferDetalle c JOIN c.personaAsociada p JOIN p.idUsuario u WHERE u.username = :username")
    Optional<ChoferDetalle> findByUsername(@Param("username") String username);
} 