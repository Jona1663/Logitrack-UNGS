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
import com.logitrack.sistema_logistica.model.enums.Estado_Envio;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, String>, JpaSpecificationExecutor<Envio> {
    
    // Forzamos a Hibernate a buscar por el nombre exacto de la variable en el modelo
    @Query("SELECT e FROM Envio e WHERE e.id_envio = :id_envio")
    Optional<Envio> buscarPorId(@Param("id_envio") String id_envio);

    // Búsqueda sin filtros
    @Query("SELECT e FROM Envio e")
    List<Envio> buscarTodos();

    // Búsqueda solo por estado
    @Query("SELECT e FROM Envio e WHERE e.estado_actual = :estado")
    List<Envio> buscarPorEstado(@Param("estado") Estado_Envio estado);

    // Búsqueda solo por fecha
        @Query("SELECT e FROM Envio e "
            + "WHERE e.fecha_creacion >= :fechaInicio "
            + "AND e.fecha_creacion < :fechaFin")
    List<Envio> buscarPorFecha(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    // Búsqueda por estado y fecha
    @Query("SELECT e FROM Envio e "
            + "WHERE e.estado_actual = :estado "
            + "AND e.fecha_creacion >= :fechaInicio "
            + "AND e.fecha_creacion < :fechaFin")
    List<Envio> buscarPorEstadoYFecha(
            @Param("estado") Estado_Envio estado,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);


    // Buscar por el nombre exacto de la variable en el modelo
    @Query(value = "SELECT * FROM envios WHERE tracking_ctg = :tracking", nativeQuery = true)
        Optional<Envio> buscarPorTracking(@Param("tracking") String tracking);
    // Devuelve envíos que no tienen chofer NI camión asignado todavía
    @Query("SELECT e FROM Envio e WHERE e.camion IS NULL AND e.chofer IS NULL " +
        "AND e.estado_actual NOT IN " +
        "(com.logitrack.sistema_logistica.model.enums.Estado_Envio.CANCELADO, " +
        "com.logitrack.sistema_logistica.model.enums.Estado_Envio.ENTREGADO)")
        List<Envio> findEnviosSinAsignar();

        //#113
        //consulta personalizada: navegar por las relaciones (desde el Envío hasta el Username del usuario).
        @Query("SELECT e FROM Envio e WHERE e.chofer.persona_asociada.id_usuario.username = :username" + 
                " AND e.estado_actual NOT IN (" +
                " com.logitrack.sistema_logistica.model.enums.Estado_Envio.ENTREGADO, " +
                " com.logitrack.sistema_logistica.model.enums.Estado_Envio.CANCELADO)")
        List<Envio> findByChoferUsername(@Param("username") String username);
}