package com.logitrack.sistema_logistica.dto;

import java.time.LocalDateTime;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoGrano;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class EnvioDetalleResponseDTO {
    private String idEnvio;
    private String cpe;
    private EstadoEnvio estadoActual;
    private TipoGrano tipoGrano;
    private Integer kgOrigen;
    private String prioridadIa;
    private String origenNombre;
    private String origenDireccion;
    private String destinoNombre;
    private String destinoDireccion;
    private String choferNombre;
    private String choferApellido;
    private LocalDateTime fechaSalida;
    private Double distanciaKm;
    private LocalDateTime fechaEstimadaLlegada;

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
                .choferNombre(envio.getChofer() != null ? envio.getChofer().getPersonaAsociada().getNombre() : "Sin asignar")
                .choferApellido(envio.getChofer() != null ? envio.getChofer().getPersonaAsociada().getApellido() : "Sin asignar")
                .fechaSalida(envio.getFechaSalida())
                .distanciaKm(envio.getDistanciaKm())
                .fechaEstimadaLlegada(etaCalculado)
                .build();
    }
}