package com.logitrack.sistema_logistica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "Establecimientos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Establecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_establecimiento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cuit_empresa", referencedColumnName = "cuit")
    private Empresa_Cliente empresa;

    @Column(name = "nombre_lugar", nullable = false, length = 100)
    private String nombre_lugar;

    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;
}