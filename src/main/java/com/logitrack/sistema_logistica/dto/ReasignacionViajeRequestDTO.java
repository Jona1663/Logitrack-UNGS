package com.logitrack.sistema_logistica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ReasignacionViajeRequestDTO {
    @NotNull(message = "El ID del nuevo chofer es obligatorio")
    private Long nuevoChoferId;

    @NotBlank(message = "La patente del nuevo camión es obligatoria")
    private String nuevoCamionId;

    @NotBlank(message = "El motivo de la reasignación es obligatorio")
    private String motivoReasignacion;
}
