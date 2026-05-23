package com.logitrack.sistema_logistica.dto;

public class ReporteEstadoDTO {
    private String estado;
    private long cantidadEnvios;
    private long kilos;

    public ReporteEstadoDTO() {}

    public ReporteEstadoDTO(String estado, long cantidadEnvios, long kilos) {
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
}
