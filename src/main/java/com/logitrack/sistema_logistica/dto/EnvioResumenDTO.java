package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoGrano;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnvioResumenDTO {
    private String idEnvio;
    private String trackingCtg;
    private String cpe; // ← agregar
    private EstadoEnvio estadoActual;
    private TipoGrano tipoGrano;
    private Integer kgOrigen;
    // private String origenNombre;
    // private String destinoNombre;
    private LugarResumenDTO origen; // Objeto anidado
    private LugarResumenDTO destino; // Objeto anidado
    private String patenteCamion; // ← agregar
    // private String nombreChofer; // ← agregar (nombre + apellido concatenados)
    private String prioridadIa; // ← agregar el campo

    @Data
    @Builder
    public static class LugarResumenDTO {
        private String nombreLugar;
        private String direccion;
    }

    // Método estático para convertir de Entidad a DTO rápidamente
    public static EnvioResumenDTO fromEntity(Envio envio) {
        return EnvioResumenDTO.builder()
                .idEnvio(envio.getIdEnvio())
                .trackingCtg(envio.getCpe())
                // .trackingCtg(envio.getTrackingCtg()) // ← corregir: era getCpe()
                .cpe(envio.getCpe()) // ← agregar
                .estadoActual(envio.getEstadoActual())
                .tipoGrano(envio.getTipoGrano())
                .kgOrigen(envio.getKgOrigen())
                .patenteCamion( // ← agregar
                        envio.getCamion() != null
                                ? envio.getCamion().getPatente()
                                : null)
                // .nombreChofer( // ← agregar
                // envio.getChofer() != null && envio.getChofer().getPersonaAsociada() != null
                // ? envio.getChofer().getPersonaAsociada().getNombre() + " "
                // + envio.getChofer().getPersonaAsociada().getApellido()
                // : null)
                .origen(LugarResumenDTO.builder()
                        .nombreLugar(envio.getOrigen().getNombreLugar())
                        .direccion(envio.getOrigen().getDireccion())
                        .build())
                .destino(LugarResumenDTO.builder()
                        .nombreLugar(envio.getDestino().getNombreLugar())
                        .direccion(envio.getDestino().getDireccion())
                        .build())
                .prioridadIa(envio.getPrioridadIa()) // ← agregar en el builder
                .build();
    }
}
