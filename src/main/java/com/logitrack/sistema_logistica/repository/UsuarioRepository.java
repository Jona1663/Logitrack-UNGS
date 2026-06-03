package com.logitrack.sistema_logistica.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.RolUsuario;
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
        Optional<Usuario> findByUsername(String username);  
        boolean existsByUsername(String username);
        List<Usuario> findByRol(RolUsuario rol);// para las notificaciones
        
}