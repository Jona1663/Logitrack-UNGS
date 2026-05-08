package com.logitrack.sistema_logistica.model;

import com.logitrack.sistema_logistica.model.enums.Estado_Envio;
import com.logitrack.sistema_logistica.model.enums.Tipo_Grano;   
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "Envios")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class Envio {

    @Id 
    @Column(name = "id_envio", length = 20) 
    private String id_envio;

    /*@Column(name = "tracking_ctg", unique = true, nullable = false, length = 50)
    private String tracking_ctg;*/ //BORRAR COLUMNA DE LA BASE DE DATOS MANUALMENTE

    @Column(name = "cpe", unique = true, length = 50)
    private String cpe;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_origen", referencedColumnName = "id_establecimiento")
    private Establecimiento origen;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_destino", referencedColumnName = "id_establecimiento")
    private Establecimiento destino;

    @Column(name = "distancia_km")
    private Double distancia_km;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_chofer", referencedColumnName = "id_chofer")
    private Chofer_Detalle chofer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patente_camion", referencedColumnName = "patente")
    private Camion camion;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_grano", nullable = false)
    private Tipo_Grano tipo_grano;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false)
    private Estado_Envio estado_actual;

    @Column(name = "prioridad_ia", length = 20)
    private String prioridad_ia;

    private Integer kg_origen;
    
    private Integer kg_destino;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fecha_creacion;

    private LocalDateTime fecha_salida;
    
    private LocalDateTime fecha_llegada;
    
    private LocalDateTime fecha_estimada_llegada;

    @Column(columnDefinition = "TEXT")
    private String comentarios;

    @PrePersist
    protected void onCreate() {
        this.fecha_creacion = LocalDateTime.now();

        if (this.id_envio == null) {
            String randomParte = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            this.id_envio = "LT-" + randomParte;
    }
    }
}

