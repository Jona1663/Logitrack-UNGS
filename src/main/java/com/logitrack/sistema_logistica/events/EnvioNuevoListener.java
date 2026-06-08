package com.logitrack.sistema_logistica.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioNuevoListener {

    private final NotificationService notificationService;

    @EventListener
    public void onEnvioCreado(EnvioNuevoEvent event) {
        log.info("[LISTENER] Nuevo envío creado → disparando email de confirmación. ID: {}",
            event.getEnvio().getIdEnvio());

        // Reutiliza notificarCambioEstado con PENDIENTE para que
        // ResendNotificationService elija la plantilla envio-creado.html
        notificationService.notificarCambioEstado(
            event.getEnvio(),
            EstadoEnvio.PENDIENTE
        );
    }
}