package com.logitrack.sistema_logistica.model;

import com.logitrack.sistema_logistica.model.enums.Tipo_Empresa;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "Empresas_Clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa_Cliente {

    @Id
    @Column(name = "cuit", length = 20)
    private String cuit;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razon_social;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_empresa", nullable = false, length = 50)
    private Tipo_Empresa tipo_empresa;

    @Column(name = "ruca_nro", length = 50)
    private String ruca_nro;

    @Builder.Default
    @Column(name = "ruca_estado")
    private Boolean ruca_estado = true;
}