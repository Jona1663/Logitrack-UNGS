package com.logitrack.sistema_logistica.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class CartaPorteDTO {
    private String idEnvio;
    private String cpe;
    private String autorizacionArca; // El CTG/CPE que viene del mock
    private String patenteCamion;
    private String nombreChofer;
    private String cuilChofer; 
    private String licenciaChofer;
    private String tipoGrano;
    private Integer pesoEstimadoKg;
    private String origen;
    private String destino;
}
