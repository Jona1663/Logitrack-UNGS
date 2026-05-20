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
    
    // De la tabla Usuario
    private Integer idUsuario;
    private String username;
    private RolUsuario rol;
    private Boolean activo;

    // De la tabla Persona
    private Integer idPersona;
    private String nombre;
    private String apellido;
    private String cuil;
    private String telefono;

    
}