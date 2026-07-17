package com.logitrack.sistema_logistica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReporteCumplimientoResponse {
    private MetricasCumplimientoDTO metricas;
    private List<ViajeCumplimientoDTO> viajes;
}

