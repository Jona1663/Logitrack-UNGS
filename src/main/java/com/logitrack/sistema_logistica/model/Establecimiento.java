package com.logitrack.sistema_logistica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "establecimientos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Establecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEstablecimiento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cuit_empresa", referencedColumnName = "cuit")
    private EmpresaCliente empresa;

    @Column(name = "nombre_lugar", nullable = false, length = 100)
    private String nombreLugar;

    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;
}