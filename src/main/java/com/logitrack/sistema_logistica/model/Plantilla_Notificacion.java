package com.logitrack.sistema_logistica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "Plantillas_Notificacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plantilla_Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_plantilla;

    @Column(unique = true, nullable = false, length = 50)
    private String codigo_evento;

    @Column(nullable = false, length = 150)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String cuerpo_mensaje;
}