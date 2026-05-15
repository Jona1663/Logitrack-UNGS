package com.logitrack.sistema_logistica.dto;

import com.logitrack.sistema_logistica.model.enums.TipoGrano;
import lombok.Data;

@Data
public class EnvioRequestDTO {
    private String idEnvio;
    private String cpe;
    private Integer idOrigen;
    private Integer idDestino;
    private Integer idChofer;
    private String patenteCamion;
    private TipoGrano tipoGrano;
    private String prioridadIa;
    private Integer kgOrigen;
    
    // Necesitamos saber qué usuario está creando esto para dejarlo asentado en el Historial
    private Integer idUsuarioCreador; 
}