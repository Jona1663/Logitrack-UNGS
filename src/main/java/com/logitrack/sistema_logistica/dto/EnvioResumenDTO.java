package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoGrano;
import lombok.Builder;
import lombok.Data;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class EnvioResumenDTO {
        private String idEnvio;
        private String trackingCtg;
        private String cpe;
        private EstadoEnvio estadoActual;
        private TipoGrano tipoGrano;
        private Integer kgOrigen;
        private LugarResumenDTO origen;
        private LugarResumenDTO destino;
        private String patenteCamion;
        private String prioridadIa;

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
                                .cpe(envio.getCpe())
                                .estadoActual(envio.getEstadoActual())
                                .tipoGrano(envio.getTipoGrano())
                                .kgOrigen(envio.getKgOrigen())
                                .patenteCamion(
                                                envio.getCamion() != null
                                                                ? envio.getCamion().getPatente()
                                                                : null)
                                .origen(LugarResumenDTO.builder()
                                                .nombreLugar(envio.getOrigen().getNombreLugar())
                                                .direccion(envio.getOrigen().getDireccion())
                                                .build())
                                .destino(LugarResumenDTO.builder()
                                                .nombreLugar(envio.getDestino().getNombreLugar())
                                                .direccion(envio.getDestino().getDireccion())
                                                .build())
                                .prioridadIa(envio.getPrioridadIa())
                                .build();
        }
}
