package com.logitrack.sistema_logistica.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrackingPublicoResponseDTO {
    private String trackingId;
    private String estadoActual;
    private String origenNombre;
    private String destinoNombre;
    private String fechaCreacion;
    private String fechaSalida;
    private String eta;
    private Integer porcentajeCompletado;
    private UbicacionDTO ubicacionActual;
}
