package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import lombok.Data;

// SOLUCIÓN TEMPORAL para editar los estados de un envío desde la vista de
// operador/supervisor

@Data
public class EnvioOperativoDTO {
    private EstadoEnvio estado;
    private String prioridadIa;
}