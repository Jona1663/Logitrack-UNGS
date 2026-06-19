package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.Envio;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnvioListadoDTO {
    // envío tal cual sale de la base
    private Envio envio;
    
    private Boolean clientePrioritario;

    public EnvioListadoDTO(Envio envio) {
        this.envio = envio;
        
        // LÓGICA DE NEGOCIO: La prioridad la define el CUIT de ORIGEN
        if (this.envio.getOrigen() != null && this.envio.getOrigen().getEmpresa() != null) {
            this.clientePrioritario = Boolean.TRUE.equals(this.envio.getOrigen().getEmpresa().getEsPrioritario());
        } else {
            this.clientePrioritario = false;
        }
    }  
}
