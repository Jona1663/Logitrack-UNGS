package com.logitrack.sistema_logistica.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoGrano;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaSalida;

    private LocalDateTime fechaLlegada;

    private LocalDateTime fechaEstimadaLlegada;

    @Column(columnDefinition = "TEXT")
    private String comentarios;

    // para que cuando se elimine un envio, tambien se elimine su ruta
    @JsonIgnore
    @OneToOne(mappedBy = "envio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RutaEnvio rutaEnvio;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();

        if (this.idEnvio == null) {
            String randomParte = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            this.idEnvio = "LT-" + randomParte;
        }
    }

    public void setEstadoEnvio(EstadoEnvio enTransito) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setEstadoEnvio'");
    }

}
