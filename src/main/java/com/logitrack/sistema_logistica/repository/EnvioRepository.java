package com.logitrack.sistema_logistica.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, String>, JpaSpecificationExecutor<Envio> {
    
    // Forzamos a Hibernate a buscar por el nombre exacto de la variable en el modelo
    @Query("SELECT e FROM Envio e WHERE e.idEnvio = :idEnvio")
    Optional<Envio> buscarPorId(@Param("idEnvio") String idEnvio);

    // Búsqueda sin filtros
    @Query("SELECT e FROM Envio e")
    List<Envio> buscarTodos();

    // Búsqueda solo por estado
    @Query("SELECT e FROM Envio e WHERE e.estadoActual = :estado")
    List<Envio> buscarPorEstado(@Param("estado") EstadoEnvio estado);

    // Búsqueda solo por fecha
        @Query("SELECT e FROM Envio e "
            + "WHERE e.fechaCreacion >= :fechaInicio "
            + "AND e.fechaCreacion < :fechaFin")
    List<Envio> buscarPorFecha(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    // Búsqueda por estado y fecha
    @Query("SELECT e FROM Envio e "
            + "WHERE e.estadoActual = :estado "
            + "AND e.fechaCreacion >= :fechaInicio "
            + "AND e.fechaCreacion < :fechaFin")
    List<Envio> buscarPorEstadoYFecha(
            @Param("estado") EstadoEnvio estado,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);


    // Buscar por el nombre exacto de la variable en el modelo
   // @Query(value = "SELECT * FROM envios WHERE trackingCtg = :tracking", nativeQuery = true)
       // Optional<Envio> buscarPorTracking(@Param("tracking") String tracking);
    
        // Devuelve envíos que no tienen chofer NI camión asignado todavía
    @Query("SELECT e FROM Envio e WHERE e.camion IS NULL AND e.chofer IS NULL " +
        "AND e.estadoActual NOT IN " +
        "(com.logitrack.sistema_logistica.model.enums.EstadoEnvio.CANCELADO, " +
        "com.logitrack.sistema_logistica.model.enums.EstadoEnvio.ENTREGADO)")
        List<Envio> findEnviosSinAsignar();

        //#113
        //consulta personalizada: navegar por las relaciones (desde el Envío hasta el Username del usuario).
        @Query("SELECT e FROM Envio e WHERE e.chofer.personaAsociada.idUsuario.username = :username" + 
                " AND e.estadoActual NOT IN (" +
                " com.logitrack.sistema_logistica.model.enums.EstadoEnvio.ENTREGADO, " +
                " com.logitrack.sistema_logistica.model.enums.EstadoEnvio.CANCELADO)")
        List<Envio> findByChoferUsername(@Param("username") String username);
}