package com.logitrack.sistema_logistica.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data

public class ReporteEnvioDetalleDTO {
    private LocalDateTime fechaEstimadaLlegada;
    private LocalDateTime fechaLlegada;
    private Long kilosTransportados;   
    private String tipoGrano;
    private String idEnvio;
    private String estado; 
}
