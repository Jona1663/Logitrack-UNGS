package com.logitrack.sistema_logistica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Entity
@Table(name = "Camiones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Camion {

    @Id
    @Column(name = "patente", length = 15)
    private String patente;

    @Column(name = "ruta_nro", length = 50)
    private String ruta_nro;

    @Column(name = "vto_senasa", nullable = false)
    private LocalDate vto_senasa;

    @Column(name = "tara_vacio_kg", nullable = false)
    private Integer tara_vacio_kg;
}