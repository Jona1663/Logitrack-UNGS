package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.HistorialEstados;
import java.time.LocalDateTime;

public class HistorialResponseDTO {

    private Integer idHistorial;
    private String idEnvio;
    private Integer idUsuario;
    private String username;
    private String estadoAnterior;
    private String estadoNuevo;
    private LocalDateTime fechaHora;

    public Integer getIdHistorial() {
        return idHistorial;
    }

    public void setIdHistorial(Integer idHistorial) {
        this.idHistorial = idHistorial;
    }

    public String getIdEnvio() {
        return idEnvio;
    }

    public void setIdEnvio(String idEnvio) {
        this.idEnvio = idEnvio;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(String estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public static HistorialResponseDTO fromEntity(HistorialEstados entidad) {
        HistorialResponseDTO dto = new HistorialResponseDTO();
        dto.setIdHistorial(entidad.getIdHistorial());
        dto.setIdEnvio(entidad.getEnvio() != null ? entidad.getEnvio().getIdEnvio() : null);
        dto.setIdUsuario(entidad.getUsuario() != null ? entidad.getUsuario().getIdUsuario() : null);
        dto.setUsername(entidad.getUsuario() != null ? entidad.getUsuario().getUsername() : null);
        dto.setEstadoAnterior(entidad.getEstadoAnterior() != null ? entidad.getEstadoAnterior().name() : null);
        dto.setEstadoNuevo(entidad.getEstadoNuevo() != null ? entidad.getEstadoNuevo().name() : null);
        dto.setFechaHora(entidad.getFechaHora());
        return dto;
    }
}
