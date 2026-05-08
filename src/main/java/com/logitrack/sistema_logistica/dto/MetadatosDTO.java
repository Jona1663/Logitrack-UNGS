package com.logitrack.sistema_logistica.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MetadatosDTO {
    private List<String> categorias;
    private List<String> estadosEnvio;
    private List<String> rolesUsuario;
    private List<String> tiposEmpresa;
    private List<String> tiposGrano;
}