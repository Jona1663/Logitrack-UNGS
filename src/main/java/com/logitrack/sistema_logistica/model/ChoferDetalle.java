package com.logitrack.sistema_logistica.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "disponible", nullable = false)
    private Boolean disponible = true;
}