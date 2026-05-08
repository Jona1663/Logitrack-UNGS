package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.Camion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CamionRepository extends JpaRepository<Camion, String> {
}