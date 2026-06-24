package com.logitrack.sistema_logistica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FatigaPendienteResponseDTO {
    // Este campo tiene que llamarse exactamente así para que el JSON
    // tenga la clave "evaluacionFatigaPendiente"
    private AlertaFatigaDTO evaluacionFatigaPendiente;
}