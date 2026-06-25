package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.enums.RolUsuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {
    private Integer idUsuario;
    private String username;
    private RolUsuario rol;
    private Boolean activo;
    private Integer idPersona;
    private String nombre;
    private String apellido;
    private String cuil;
    private String telefono;

}