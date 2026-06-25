package com.logitrack.sistema_logistica.model;

import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import lombok.Builder;
import com.logitrack.sistema_logistica.model.enums.TipoEvento;

@Entity
@Table(name = "historial_Estados")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialEstados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idHistorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_envio", referencedColumnName = "id_envio")
    private Envio envio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento")
    private TipoEvento tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 50)
    private EstadoEnvio estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 50)
    private EstadoEnvio estadoNuevo;

    @Column(name = "fecha_hora", updatable = false)
    private LocalDateTime fechaHora;

    @Column(name = "detalle", length = 255)
    private String detalle;

    @PrePersist
    protected void onCreate() {
        this.fechaHora = LocalDateTime.now();
    }
}