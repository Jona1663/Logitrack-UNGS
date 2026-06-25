package com.logitrack.sistema_logistica.dto;

public class ReporteTotalesDTO {
    private long totalViajes;
    private long totalKilos;

    public ReporteTotalesDTO() {
    }

    public ReporteTotalesDTO(long totalViajes, long totalKilos) {
        this.totalViajes = totalViajes;
        this.totalKilos = totalKilos;
    }

    public long getTotalViajes() {
        return totalViajes;
    }

    public void setTotalViajes(long totalViajes) {
        this.totalViajes = totalViajes;
    }

    public long getTotalKilos() {
        return totalKilos;
    }

    public void setTotalKilos(long totalKilos) {
        this.totalKilos = totalKilos;
    }
}
