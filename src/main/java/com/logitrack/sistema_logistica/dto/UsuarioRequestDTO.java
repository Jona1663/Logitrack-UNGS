package com.logitrack.sistema_logistica.dto;

import java.time.LocalDate;

import com.logitrack.sistema_logistica.model.enums.RolUsuario;

import jakarta.validation.constraints.Email;
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
public class UsuarioRequestDTO {
    @NotBlank(message = "El email/username es obligatorio")
    @Email(message = "Formato de email inválido")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    @NotBlank(message = "El CUIL es obligatorio")
    private String cuil;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    private String telefono;

    private String nroLicencia;
    private LocalDate vtoLicencia;
    private LocalDate vtoLinti;

}