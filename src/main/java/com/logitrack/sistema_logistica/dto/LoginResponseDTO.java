package com.logitrack.sistema_logistica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private Integer id;
    private String token;
    private String rol;
    private String username;
}