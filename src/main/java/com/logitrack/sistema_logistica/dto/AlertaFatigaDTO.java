package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.enums.EstadoEvaluacionEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter 
@AllArgsConstructor 
@NoArgsConstructor  
@ToString
public class AlertaFatigaDTO {
    private String idEnvio;
    private String nombreChofer;
    private String motivo;
    private Long idEvaluacion;
    private EstadoEvaluacionEnum estadoBloqueo;
}
