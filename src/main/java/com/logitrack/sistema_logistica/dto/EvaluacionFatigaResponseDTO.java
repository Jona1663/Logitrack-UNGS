package com.logitrack.sistema_logistica.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionFatigaResponseDTO {
    private Long idEvaluacion;
    private boolean aprobado;
    private String mensaje;

}
