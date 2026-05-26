package com.logitrack.sistema_logistica.model;

import java.time.LocalDate;

import com.logitrack.sistema_logistica.model.enums.TipoEmpresa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "empresas_Clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaCliente {

    @Id
    @Column(name = "cuit")
    private String cuit;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_empresa", nullable = false, length = 50)
    private TipoEmpresa tipoEmpresa;

    @Column(name = "ruca_nro", length = 50)
    private String rucaNro;

    @Column(name = "vto_ruca")
    private LocalDate vtoRuca;
}