package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.AlertaWeb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertaWebRepository extends JpaRepository<AlertaWeb, Integer> {
    List<AlertaWeb> findByUsuarioIdUsuarioAndLeidoFalseOrderByFechaHoraDesc(Integer idUsuario);
}