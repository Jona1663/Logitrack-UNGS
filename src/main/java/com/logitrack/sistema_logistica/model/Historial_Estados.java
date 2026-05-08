package com.logitrack.sistema_logistica.model;

import com.logitrack.sistema_logistica.model.enums.Estado_Envio;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import lombok.Builder;

@Entity
@Table(name = "Historial_Estados")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Historial_Estados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_historial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_envio", referencedColumnName = "id_envio")
    private Envio envio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 50)
    private Estado_Envio estado_anterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 50)
    private Estado_Envio estado_nuevo;

    @Column(name = "fecha_hora", updatable = false)
    private LocalDateTime fecha_hora;

    @PrePersist
    protected void onCreate() {
        this.fecha_hora = LocalDateTime.now();
    }
}