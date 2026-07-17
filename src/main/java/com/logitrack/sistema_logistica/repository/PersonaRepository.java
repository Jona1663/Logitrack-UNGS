package com.logitrack.sistema_logistica.repository;

import java.util.Optional;
import com.logitrack.sistema_logistica.model.Persona;
import com.logitrack.sistema_logistica.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Integer> {
    Optional<Persona> findByIdUsuario(Usuario usuario);


}