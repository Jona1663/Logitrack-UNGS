package com.logitrack.sistema_logistica.dto;

import java.time.LocalDateTime;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoGrano;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnvioDetalleResponseDTO {

    private String idEnvio;
    private String cpe;
    private EstadoEnvio estadoActual;
    private TipoGrano tipoGrano;
    private Integer kgOrigen;
    private String prioridadIa;

    // Datos de origen y destino
    private String origenNombre;
    private String origenDireccion;
    private String destinoNombre;
    private String destinoDireccion;

    // Datos del chofer
    private String choferNombre;
    private String choferApellido;

    // --- Campos para #121 y #122 --
    private LocalDateTime fechaSalida;
    private Double distanciaKm;
    private LocalDateTime fechaEstimadaLlegada; // Hora Estimada de Llegada calculada

    // Método estático de conversión: recibe la entidad + el ETA ya calculado
    public static EnvioDetalleResponseDTO fromEntity(Envio envio, LocalDateTime etaCalculado) {
        return EnvioDetalleResponseDTO.builder()
                .idEnvio(envio.getIdEnvio())
                .cpe(envio.getCpe())
                .estadoActual(envio.getEstadoActual())
                .tipoGrano(envio.getTipoGrano())
                .kgOrigen(envio.getKgOrigen())
                .prioridadIa(envio.getPrioridadIa())
                .origenNombre(envio.getOrigen().getNombreLugar())
                .origenDireccion(envio.getOrigen().getDireccion())
                .destinoNombre(envio.getDestino().getNombreLugar())
                .destinoDireccion(envio.getDestino().getDireccion())
                .choferNombre(envio.getChofer().getPersonaAsociada().getNombre())
                .choferApellido(envio.getChofer().getPersonaAsociada().getApellido())
                .fechaSalida(envio.getFechaSalida())
                .distanciaKm(envio.getDistanciaKm())
                .fechaEstimadaLlegada(envio.getFechaEstimadaLlegada())
                .build();
    }
}