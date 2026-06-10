package com.logitrack.sistema_logistica.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder

public class CumplimientoMetricasDTO {
    private long totalEntregados;
    private long entregadosATiempo;
    private long conRetraso;
    private double porcentajeATiempo;
    private double porcentajeConRetraso;
}
