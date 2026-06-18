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
    // Indica si el chofer puede iniciar el viaje o si debe reintentar/esperar
    private boolean aprobado;
    
    // Mensaje para mostrar en el frontend (ej: "Test superado", "Fatiga detectada: viaje bloqueado")
    private String mensaje;
}
