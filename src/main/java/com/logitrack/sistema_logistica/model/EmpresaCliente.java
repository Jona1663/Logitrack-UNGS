package com.logitrack.sistema_logistica.model;

import com.logitrack.sistema_logistica.model.enums.TipoEmpresa;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

@Entity
@Table(name = "empresas_Clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaCliente {

    @Id
    @Column(name = "cuit", length = 20)
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