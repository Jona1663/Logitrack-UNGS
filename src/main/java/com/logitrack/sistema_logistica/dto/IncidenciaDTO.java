package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.enums.TipoIncidencia;
import lombok.Data;

@Data
public class IncidenciaDTO {
    private TipoIncidencia tipoIncidencia;
    private String descripcion;
}