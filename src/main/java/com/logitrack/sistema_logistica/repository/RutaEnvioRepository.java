package com.logitrack.sistema_logistica.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.RutaEnvio;

@Repository
public interface RutaEnvioRepository extends JpaRepository<RutaEnvio, Long> {

    Optional<RutaEnvio> findByEnvio_IdEnvio(String idEnvio);
    Optional<RutaEnvio> findByEnvio(Envio envio);
}