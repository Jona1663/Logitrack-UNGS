package com.logitrack.sistema_logistica.service;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;

public interface NotificationService {

    // Método original — usado por IncidenciaService para alertar al supervisor
    void enviarNotificacion(String destinatario, String asunto, String mensaje);

    // Método nuevo — usado por EnvioCambioEstadoListener para notificar al cliente
    void notificarCambioEstado(Envio envio, EstadoEnvio nuevoEstado);
}