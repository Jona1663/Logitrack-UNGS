package com.logitrack.sistema_logistica.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EstadoUpdateResponseDTO {
    private String mensaje;
    private String estadoActual;
    private LocalDateTime fechaActualizacion;
}