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
    // Identificadores del viaje y autorizaciones
    private String idEnvio;
    private String cpe;
    private String autorizacionArca; // El CTG/CPE que viene del mock

    // Datos del transporte
    private String patenteCamion;
    
    // Datos del chofer (para cruzar con su registro)
    private String nombreChofer;
    private String cuilChofer; 
    private String licenciaChofer;

    // Datos de la carga
    private String tipoGrano;
    private Integer pesoEstimadoKg;

    // Ruta
    private String origen;
    private String destino;
}
