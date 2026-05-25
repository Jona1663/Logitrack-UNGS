package com.logitrack.sistema_logistica.model;

import java.time.LocalDateTime;

import com.logitrack.sistema_logistica.model.enums.Categoria;
import com.logitrack.sistema_logistica.model.enums.EstadoIncidencia;
import com.logitrack.sistema_logistica.model.enums.TipoIncidencia;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "incidencias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idIncidencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_envio", referencedColumnName = "id_envio")
    private Envio envio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoIncidencia tipoIncidencia;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoIncidencia estado;

    @Column(nullable = false)
    private LocalDateTime fechaReporte;

    // Estos campos se llenan cuando el supervisor interviene
    private LocalDateTime fechaResolucion;

    @Column(length = 500)
    private String notasSupervisor;

    @Column(columnDefinition = "TEXT")//actualmente es un text, si en algun momento decidimos implementar 
    private String lugarIncidencia;   // mostrar en el mapa la ubicacion de la incidencia podemos dejarlo como coordenadas 

    @Column(nullable = false, updatable = false)
    private String creadoPor;


}