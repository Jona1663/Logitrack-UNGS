package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.enums.Estado_Envio;
import lombok.Data;

// SOLUCIÓN TEMPORAL para editar los estados de un envío desde la vista de
// operador/supervisor

@Data
public class EnvioOperativoDTO {
    private Estado_Envio estado;
    private String prioridad_ia;
}