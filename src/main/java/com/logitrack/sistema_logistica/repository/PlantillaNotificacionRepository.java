package com.logitrack.sistema_logistica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logitrack.sistema_logistica.model.PlantillaNotificacion;

@Repository
public interface PlantillaNotificacionRepository extends JpaRepository<PlantillaNotificacion, Integer> {
}