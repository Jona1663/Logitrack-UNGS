package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.RutaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RutaEnvioRepository extends JpaRepository<RutaEnvio, Long> {

    Optional<RutaEnvio> findByEnvio_IdEnvio(String idEnvio);
}