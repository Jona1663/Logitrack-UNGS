package com.logitrack.sistema_logistica.dto;

import lombok.Data;

@Data
public class EstadoUpdateRequestDTO {
    private String nuevoEstado; // Debe coincidir con el JSON: { "nuevoEstado": "..." } [cite: 43-44]
}