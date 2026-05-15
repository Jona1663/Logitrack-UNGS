package com.logitrack.sistema_logistica.model;

import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoGrano;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "envios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Envio {

    @Id
    @Column(name = "id_Envio", length = 20)
    private String idEnvio;

    /*
     * @Column(name = "tracking_ctg", unique = true, nullable = false, length = 50)
     * private String trackingCtg;
     */ // BORRAR COLUMNA DE LA BASE DE DATOS MANUALMENTE

    @Column(name = "cpe", unique = true, length = 50)
    private String cpe;

    @Column(name = "autorizacion_ARCA", length = 50)
    private String autorizacionARCA;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_origen")
    private Establecimiento origen;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_destino")
    private Establecimiento destino;

    @Column(name = "distancia_km")
    private Double distanciaKm;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_chofer", referencedColumnName = "id_chofer")
    private ChoferDetalle chofer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patente_camion", referencedColumnName = "patente")
    private Camion camion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_grano", nullable = false)
    private TipoGrano tipoGrano;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false)
    private EstadoEnvio estadoActual;

    @Column(name = "prioridad_ia", length = 20)
    private String prioridadIa;

    private Integer kgOrigen;

    private Integer kgDestino;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaSalida;

    private LocalDateTime fechaLlegada;

    private LocalDateTime fechaEstimadaLlegada;

    @Column(columnDefinition = "TEXT")
    private String comentarios;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();

        if (this.idEnvio == null) {
            String randomParte = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            this.idEnvio = "LT-" + randomParte;
        }
    }
}
