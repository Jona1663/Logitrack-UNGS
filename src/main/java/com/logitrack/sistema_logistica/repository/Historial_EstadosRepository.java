package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.Historial_Estados;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface Historial_EstadosRepository extends JpaRepository<Historial_Estados, Integer> {
    
    // Usamos @Query para evitar el lío de los nombres automáticos con guiones bajos
    // JOIN FETCH se agrega para traer la información de envío y usuario junto con el historial
    @Query("SELECT h FROM Historial_Estados h JOIN FETCH h.envio e JOIN FETCH h.usuario u WHERE e.id_envio = :idEnvio ORDER BY h.fecha_hora DESC")
    List<Historial_Estados> buscarHistorialPorEnvio(@Param("idEnvio") String idEnvio);
}