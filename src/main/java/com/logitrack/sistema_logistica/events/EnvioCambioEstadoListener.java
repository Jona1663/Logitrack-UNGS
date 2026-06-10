package com.logitrack.sistema_logistica.events;

import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioCambioEstadoListener {

    private final NotificationService notificationService;

    private static final Set<EstadoEnvio> ESTADOS_NOTIFICABLES = Set.of(
        EstadoEnvio.PENDIENTE,       // email de confirmación de creación
        EstadoEnvio.EN_TRANSITO,
        EstadoEnvio.EN_PUNTO_DE_RECOLECCION,
        EstadoEnvio.EN_REPARTO,
        EstadoEnvio.ENTREGADO,
        EstadoEnvio.CANCELADO        // email de cancelación
    );

    @EventListener
    public void onCambioEstado(EnvioCambioEstadoEvent event) {
        if (!ESTADOS_NOTIFICABLES.contains(event.getNuevoEstado())) {
            log.debug("[LISTENER] Estado {} no dispara notificación. Envío: {}",
                event.getNuevoEstado(), event.getEnvio().getIdEnvio());
            return;
        }

        log.info("[LISTENER] Disparando notificación → envío {} | estado: {}",
            event.getEnvio().getIdEnvio(), event.getNuevoEstado());

        notificationService.notificarCambioEstado(event.getEnvio(), event.getNuevoEstado());
    }
}