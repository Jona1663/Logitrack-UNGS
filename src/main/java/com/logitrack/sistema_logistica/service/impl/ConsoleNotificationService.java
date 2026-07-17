package com.logitrack.sistema_logistica.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.service.NotificationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "resend.enabled", havingValue = "false")
public class ConsoleNotificationService implements NotificationService {

    // notificación al supervisor por incidencia
    @Override
    public void enviarNotificacion(String destinatario, String asunto, String mensaje) {
        log.info("""
            [MAIL-CONSOLE] Notificación simulada
              → Para   : {}
              → Asunto : {}
              → Mensaje: {}
            """,
            destinatario, asunto, mensaje
        );
    }

    //notificación al cliente por cambio de estado
    @Override
    public void notificarCambioEstado(Envio envio, EstadoEnvio nuevoEstado) {
        String origenNombre  = envio.getOrigen()  != null ? envio.getOrigen().toString()  : "—";
        String destinoNombre = envio.getDestino() != null ? envio.getDestino().toString() : "—";

        log.info("""
            [MAIL-CONSOLE] Notificación simulada (Resend desactivado)
              → Envío ID : {}
              → Estado   : {}
              → Origen   : {}
              → Destino  : {}
            """,
            envio.getIdEnvio(),
            nuevoEstado,
            origenNombre,
            destinoNombre
        );
    }
}