package com.logitrack.sistema_logistica.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChoferAlertaDTO {
    private Integer id;
    private String nombreCompleto;
    private String telefono;
}