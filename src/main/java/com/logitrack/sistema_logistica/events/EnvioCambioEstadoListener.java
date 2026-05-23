package com.logitrack.sistema_logistica.events;

import com.logitrack.sistema_logistica.model.enums.PlantillaNotificacion;
import com.logitrack.sistema_logistica.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EnvioCambioEstadoListener {

    private static final Logger log = LoggerFactory.getLogger(EnvioCambioEstadoListener.class);
    private final NotificationService notificationService;

    public EnvioCambioEstadoListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void onCambioEstado(EnvioCambioEstadoEvent event) {
        try {
            var envio = event.getEnvio();
            PlantillaNotificacion plantilla = PlantillaNotificacion.valueOf(envio.getEstadoActual().name());
            String cliente = envio.getOrigen().getEmpresa().getRazonSocial();
            String mensaje = plantilla.getCuerpo(cliente, envio.getIdEnvio());
            notificationService.enviarNotificacion(
                "cliente-fijo@logitrack.com",
                plantilla.getAsunto(),
                mensaje
            );
        } catch (Exception e) {
            log.warn("No se pudo enviar notificación para envío {}: {}",
                event.getEnvio().getIdEnvio(), e.getMessage());
        }
    }
}