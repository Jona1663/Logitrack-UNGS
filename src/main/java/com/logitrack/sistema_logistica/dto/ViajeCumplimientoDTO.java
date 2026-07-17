package com.logitrack.sistema_logistica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViajeCumplimientoDTO {
    private String idEnvio;
    private String estadoActual;
    private LocalDateTime fechaEstimadaLlegada;
    private LocalDateTime fechaEntregaReal;
    private boolean esRetrasado;
    private double desvioHoras; // Aca se guardan el cálculo
}

