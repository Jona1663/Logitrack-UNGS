package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.Estado_Envio;
import com.logitrack.sistema_logistica.model.enums.Tipo_Grano;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnvioResumenDTO {
    private String id_envio;
    private String tracking_ctg;
    private String cpe; // ← agregar
    private Estado_Envio estado_actual;
    private Tipo_Grano tipo_grano;
    private Integer kg_origen;
    // private String origen_nombre;
    // private String destino_nombre;
    private LugarResumenDTO origen; // Objeto anidado
    private LugarResumenDTO destino; // Objeto anidado
    private String patente_camion; // ← agregar
    // private String nombre_chofer; // ← agregar (nombre + apellido concatenados)
    private String prioridad_ia; // ← agregar el campo

    @Data
    @Builder
    public static class LugarResumenDTO {
        private String nombre_lugar;
        private String direccion;
    }

    // Método estático para convertir de Entidad a DTO rápidamente
    public static EnvioResumenDTO fromEntity(Envio envio) {
        return EnvioResumenDTO.builder()
                .id_envio(envio.getId_envio())
                .tracking_ctg(envio.getCpe())
                // .tracking_ctg(envio.getTracking_ctg()) // ← corregir: era getCpe()
                .cpe(envio.getCpe()) // ← agregar
                .estado_actual(envio.getEstado_actual())
                .tipo_grano(envio.getTipo_grano())
                .kg_origen(envio.getKg_origen())
                .patente_camion( // ← agregar
                        envio.getCamion() != null
                                ? envio.getCamion().getPatente()
                                : null)
                // .nombre_chofer( // ← agregar
                // envio.getChofer() != null && envio.getChofer().getPersona_asociada() != null
                // ? envio.getChofer().getPersona_asociada().getNombre() + " "
                // + envio.getChofer().getPersona_asociada().getApellido()
                // : null)
                .origen(LugarResumenDTO.builder()
                        .nombre_lugar(envio.getOrigen().getNombre_lugar())
                        .direccion(envio.getOrigen().getDireccion())
                        .build())
                .destino(LugarResumenDTO.builder()
                        .nombre_lugar(envio.getDestino().getNombre_lugar())
                        .direccion(envio.getDestino().getDireccion())
                        .build())
                .prioridad_ia(envio.getPrioridad_ia()) // ← agregar en el builder
                .build();
    }
}
