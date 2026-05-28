package com.logitrack.sistema_logistica.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para el alta de un nuevo cliente (US-38).
 * El backend valida los datos antes de persistirlos.
 */
public record ClienteRequestDTO(

    @NotBlank(message = "El CUIT es obligatorio")

    String cuit,

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 150, message = "La razón social no puede superar los 150 caracteres")
    String razonSocial,

    @NotBlank(message = "El tipo de empresa es obligatorio")
    String tipoEmpresa,

    @Email(message = "El formato del email no es válido")
    String email,
    
    // Campos opcionales
    String rucaNro,
    String vtoRuca,    // Se recibe como String "YYYY-MM-DD" y se convierte en el servicio
    
    SedeDTO sede  // nullable — es opcional
) {
    public record SedeDTO(
        String nombreLugar,
        String direccion,
        Double latitud,
        Double longitud
    ) {}
}