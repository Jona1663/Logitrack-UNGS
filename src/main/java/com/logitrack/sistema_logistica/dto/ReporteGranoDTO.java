package com.logitrack.sistema_logistica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReporteGranoDTO {
    private String tipoGrano;
    private Long cantidadEnvios;
    private Long totalKilos;
}