package com.logitrack.sistema_logistica.events;

import com.logitrack.sistema_logistica.dto.ViajeEstadoUpdateDTO;
import com.logitrack.sistema_logistica.model.enums.PlantillaNotificacion;
import com.logitrack.sistema_logistica.service.NotificationService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component

public class EnvioCambioEstadoListenerNotificaciones {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Logger log = LoggerFactory.getLogger(EnvioCambioEstadoListenerNotificaciones.class);
    private final NotificationService notificationService;

    public EnvioCambioEstadoListenerNotificaciones(NotificationService notificationService) {
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
                    mensaje);
        } catch (Exception e) {
            log.warn("No se pudo enviar notificación para envío {}: {}",
                    event.getEnvio().getIdEnvio(), e.getMessage());
        }
        try {
            var envio = event.getEnvio();
            // Construimos un nombre seguro para el chofer por si viene nulo
            String nombreChofer = "No asignado";
            if (envio.getChofer() != null && envio.getChofer().getPersonaAsociada() != null) {
                nombreChofer = envio.getChofer().getPersonaAsociada().getNombre() + " " +
                        envio.getChofer().getPersonaAsociada().getApellido();
            }

            // Armamos el DTO liviano
            ViajeEstadoUpdateDTO updatePayload = ViajeEstadoUpdateDTO.builder()
                    .idEnvio(envio.getIdEnvio())
                    .estadoNuevo(envio.getEstadoActual().name())
                    .patenteCamion(envio.getCamion() != null ? envio.getCamion().getPatente() : "S/D")
                    .choferNombre(nombreChofer)
                    .fechaHora(LocalDateTime.now())
                    .build();

            // Despachamos al tópico GLOBAL.
            // Cualquiera que esté mirando el dashboard va a estar escuchando este canal.
            messagingTemplate.convertAndSend("/topic/viajes", updatePayload);

            System.out.println("[WebSocket] Estado de viaje " + envio.getIdEnvio() + " transmitido a /topic/viajes");

        } catch (Exception e) {
            System.err.println("Error transmitiendo actualización de viaje por socket: " + e.getMessage());
        }

    }
}