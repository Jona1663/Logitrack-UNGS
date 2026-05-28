package com.logitrack.sistema_logistica.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor


public class ReporteEstadoDTO {
    private String estado;
    private Long cantidadEnvios;
    private Long kilos;

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

}
