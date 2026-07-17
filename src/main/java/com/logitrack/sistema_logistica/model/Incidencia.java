package com.logitrack.sistema_logistica.model;

import com.logitrack.sistema_logistica.model.enums.EstadoIncidencia;
import com.logitrack.sistema_logistica.model.enums.TipoIncidencia;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
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

    private LocalDateTime fechaResolucion;

    @Column(length = 500)
    private String notasSupervisor;

    @Column(columnDefinition = "TEXT")
    private String lugarIncidencia;   

    @Column(nullable = false, updatable = false)
    private String creadoPor;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;


}