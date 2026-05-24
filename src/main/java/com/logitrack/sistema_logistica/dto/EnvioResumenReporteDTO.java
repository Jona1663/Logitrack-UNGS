package com.logitrack.sistema_logistica.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.logitrack.sistema_logistica.model.Envio;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnvioResumenReporteDTO {
    private String idEnvio;
    private String estado;
    private String tipoGrano;
    private Integer kilosTotales;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaLlegada;

    public static EnvioResumenReporteDTO fromEntity(Envio envio) {
        return EnvioResumenReporteDTO.builder()
                .idEnvio(envio.getIdEnvio())
                .estado(envio.getEstadoActual().name())
                .tipoGrano(envio.getTipoGrano().name())
                .kilosTotales(envio.getKgOrigen())
                .fechaCreacion(envio.getFechaCreacion())
                .fechaSalida(envio.getFechaSalida())
                .fechaLlegada(envio.getFechaLlegada())
                .build();
    }
}