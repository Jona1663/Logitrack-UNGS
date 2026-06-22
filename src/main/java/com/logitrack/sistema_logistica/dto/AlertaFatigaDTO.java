package com.logitrack.sistema_logistica.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter // Importante: siempre es bueno tener setters por si acaso
@AllArgsConstructor // Genera el constructor con los 3 campos automáticamente
@NoArgsConstructor  // Necesario para que Spring pueda deserializar el JSON si hace falta
@ToString//Para probar log en consola
public class AlertaFatigaDTO {
    private String idEnvio;
    private String nombreChofer;
    private String motivo;
    private Long idEvaluacion;
}
