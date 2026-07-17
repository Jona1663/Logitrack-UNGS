package com.logitrack.sistema_logistica.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidenciaMapaDTO {
    private Double latitud;
    private Double longitud;
    private String estado;        
    private String patenteCamion;
    private String nombreChofer;
    private String descripcion;
    private LocalDateTime fechaReporte;
}
