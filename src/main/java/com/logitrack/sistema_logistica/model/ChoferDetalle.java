package com.logitrack.sistema_logistica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Entity
@Table(name = "choferes_Detalle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoferDetalle {

    @Id
    private Integer idChofer;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "id_chofer")
    private Persona personaAsociada;

    @Column(name = "nro_licencia", nullable = false, length = 50)
    private String nroLicencia;

    @Column(name = "vto_licencia", nullable = false)
    private LocalDate vtoLicencia;

    @Column(name = "vto_linti", nullable = false)
    private LocalDate vtoLinti;
}