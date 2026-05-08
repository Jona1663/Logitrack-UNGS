package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.Historial_Estados;
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

    public static HistorialResponseDTO fromEntity(Historial_Estados entidad) {
        HistorialResponseDTO dto = new HistorialResponseDTO();
        dto.setIdHistorial(entidad.getId_historial());
        dto.setIdEnvio(entidad.getEnvio() != null ? entidad.getEnvio().getId_envio() : null);
        dto.setIdUsuario(entidad.getUsuario() != null ? entidad.getUsuario().getId_usuario() : null);
        dto.setUsername(entidad.getUsuario() != null ? entidad.getUsuario().getUsername() : null);
        dto.setEstadoAnterior(entidad.getEstado_anterior() != null ? entidad.getEstado_anterior().name() : null);
        dto.setEstadoNuevo(entidad.getEstado_nuevo() != null ? entidad.getEstado_nuevo().name() : null);
        dto.setFechaHora(entidad.getFecha_hora());
        return dto;
    }
}
