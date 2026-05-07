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
    private Estado_Envio estado_actual;
    private Tipo_Grano tipo_grano;
    private Integer kg_origen;
    //private String origen_nombre;
    //private String destino_nombre;
    private LugarResumenDTO origen;  // Objeto anidado
    private LugarResumenDTO destino; // Objeto anidado

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
                .estado_actual(envio.getEstado_actual())
                .tipo_grano(envio.getTipo_grano())
                .kg_origen(envio.getKg_origen())
                .origen(LugarResumenDTO.builder()
                        .nombre_lugar(envio.getOrigen().getNombre_lugar())
                        .direccion(envio.getOrigen().getDireccion())
                        .build())
                .destino(LugarResumenDTO.builder()
                        .nombre_lugar(envio.getDestino().getNombre_lugar())
                        .direccion(envio.getDestino().getDireccion())
                        .build())
                .build();
    }
}
