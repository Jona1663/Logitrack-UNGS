package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.enums.TipoJuegoEnum;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter

public class EvaluacionFatigaRequestDTO {
    private String envioId;
    private TipoJuegoEnum tipoJuego;
    private Long tiempoReaccionMs;
}
