package com.logitrack.sistema_logistica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViajeEstadoUpdateDTO {
    private String idEnvio;
    private String estadoNuevo;
    private String choferNombre;
    private String patenteCamion;
    private LocalDateTime fechaHora;
}