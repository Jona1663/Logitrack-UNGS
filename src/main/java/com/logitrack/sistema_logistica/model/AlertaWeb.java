package com.logitrack.sistema_logistica.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas_web")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaWeb {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAlertaWeb;

    private String mensaje;
    private String tipo; 
    private boolean leido;
    private LocalDateTime fechaHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario; 
}