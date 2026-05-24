package com.logitrack.sistema_logistica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetricasCumplimientoDTO {
    private long totalEntregados;
    private long entregadosATiempo;
    private long entregadosConRetraso;
    private double porcentajeATiempo;
    private double porcentajeRetraso;
}