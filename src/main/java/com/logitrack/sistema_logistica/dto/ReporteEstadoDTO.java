package com.logitrack.sistema_logistica.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class ReporteEstadoDTO {
    private String estado;
    private Long cantidadEnvios;
    private Long kilos;
    private Long capacidadCargaKg;

    // ESTE ES EL CONSTRUCTOR QUE HIBERNATE NECESITA PARA TUS 3 PARÁMETROS
    public ReporteEstadoDTO(String estado, Long cantidadEnvios, Long kilos) {
        this.estado = estado;
        this.cantidadEnvios = cantidadEnvios;
        this.kilos = kilos;
    }


    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public long getCantidadEnvios() {
        return cantidadEnvios;
    }

    public void setCantidadEnvios(long cantidadEnvios) {
        this.cantidadEnvios = cantidadEnvios;
    }

    public long getKilos() {
        return kilos;
    }

    public void setKilos(long kilos) {
        this.kilos = kilos;
    }

    public Long getCapacidadCargaKg() {
        return capacidadCargaKg;
    }       

}
