package com.logitrack.sistema_logistica.events;

import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MODIFICAR — events/EnvioCambioEstadoListener.java
 *
 * Cambios respecto a la versión anterior:
 * - Se agrega PENDIENTE al set de estados notificables (email de creación de envío)
 * - Se agrega CANCELADO al set de estados notificables (email de cancelación)
 * - Ahora notifica los 5 estados relevantes:
 *     PENDIENTE, EN_TRANSITO, EN_REPARTO, ENTREGADO, CANCELADO
 * - EN_PUNTO_DE_RECOLECCION sigue sin notificación (no se requiere)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioCambioEstadoListener {

    private final NotificationService notificationService;

    private static final Set<EstadoEnvio> ESTADOS_NOTIFICABLES = Set.of(
        EstadoEnvio.PENDIENTE,       // nuevo — email de confirmación de creación
        EstadoEnvio.EN_TRANSITO,
        EstadoEnvio.EN_PUNTO_DE_RECOLECCION,
        EstadoEnvio.EN_REPARTO,
        EstadoEnvio.ENTREGADO,
        EstadoEnvio.CANCELADO        // nuevo — email de cancelación
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