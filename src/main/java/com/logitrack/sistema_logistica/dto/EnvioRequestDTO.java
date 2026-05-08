package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.enums.Tipo_Grano;
import lombok.Data;

@Data
public class EnvioRequestDTO {
    private String id_envio;
    private String cpe;
    private Integer id_origen;
    private Integer id_destino;
    private Integer id_chofer;
    private String patente_camion;
    private Tipo_Grano tipo_grano;
    private String prioridad_ia;
    private Integer kg_origen;
    
    // Necesitamos saber qué usuario está creando esto para dejarlo asentado en el Historial
    private Integer id_usuario_creador; 
}