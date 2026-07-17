package com.logitrack.sistema_logistica.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

// tabla de datos para almacenar la ruta de cada envio
@Entity
@Table(name = "rutas_envio")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Long idRuta;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_envio", referencedColumnName = "id_Envio")
    private Envio envio;

    // json con las coordenadas de la ruta
    // es una lista gigante de coordenadas
    @Column(name = "polyline_json", columnDefinition = "TEXT")
    private String polylineJson;

    @Column(name = "distancia_total_km")
    private Double distanciaTotalKm;

    @Column(name = "duracion_total_segundos")
    private Long duracionTotalSegundos;
}