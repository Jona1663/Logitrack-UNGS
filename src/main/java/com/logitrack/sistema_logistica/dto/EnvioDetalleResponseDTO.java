package com.logitrack.sistema_logistica.dto;


import java.time.LocalDateTime;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.Estado_Envio;
import com.logitrack.sistema_logistica.model.enums.Tipo_Grano;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnvioDetalleResponseDTO {

    private String id_envio;
    private String cpe;
    private Estado_Envio estado_actual;
    private Tipo_Grano tipo_grano;
    private Integer kg_origen;

    // Datos de origen y destino
    private String origen_nombre;
    private String origen_direccion;
    private String destino_nombre;
    private String destino_direccion;

    // Datos del chofer
    private String chofer_nombre;
    private String chofer_apellido;

    // --- Campos para #121 y #122 --
    private LocalDateTime fecha_salida;
    private Double distancia_km;
    private LocalDateTime Fecha_estimada_llegada; // Hora Estimada de Llegada calculada

    // Método estático de conversión: recibe la entidad + el ETA ya calculado
    public static EnvioDetalleResponseDTO fromEntity(Envio envio, LocalDateTime etaCalculado) {
        return EnvioDetalleResponseDTO.builder()
                .id_envio(envio.getId_envio())
                .cpe(envio.getCpe())
                .estado_actual(envio.getEstado_actual())
                .tipo_grano(envio.getTipo_grano())
                .kg_origen(envio.getKg_origen())
                .origen_nombre(envio.getOrigen().getNombre_lugar())
                .origen_direccion(envio.getOrigen().getDireccion())
                .destino_nombre(envio.getDestino().getNombre_lugar())
                .destino_direccion(envio.getDestino().getDireccion())
                .chofer_nombre(envio.getChofer().getPersona_asociada().getNombre())
                .chofer_apellido(envio.getChofer().getPersona_asociada().getApellido())
                .fecha_salida(envio.getFecha_salida())
                .distancia_km(envio.getDistancia_km())
                .Fecha_estimada_llegada(envio.getFecha_estimada_llegada())
                .build();
    }
}