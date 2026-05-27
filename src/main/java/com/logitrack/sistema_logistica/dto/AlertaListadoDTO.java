package com.logitrack.sistema_logistica.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertaListadoDTO {
    private Integer id;
    private String idEnvio;
    private ChoferAlertaDTO chofer;
    private String tipoIncidencia;
    private String descripcion;
    private String estado;
    private LocalDateTime fechaReporte;
    private LocalDateTime fechaResolucion;
    private String lugarIncidencia; // Agregamos la ubicación para el front!
    private Long capacidadCargaKg;
    
}