package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Integer> {
}