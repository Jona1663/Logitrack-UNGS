package com.logitrack.sistema_logistica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "plantillas_notificacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaNotificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPlantilla;

    @Column(unique = true, nullable = false, length = 50)
    private String codigoEvento;

    @Column(nullable = false, length = 150)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String cuerpoMensaje;
}