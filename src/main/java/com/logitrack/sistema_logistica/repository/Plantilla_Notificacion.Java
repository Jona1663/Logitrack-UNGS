package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.Plantilla_Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Plantilla_NotificacionRepository extends JpaRepository<Plantilla_Notificacion, Integer> {
}