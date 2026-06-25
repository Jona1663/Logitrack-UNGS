package com.logitrack.sistema_logistica.events;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioNuevoListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnvioCreado(EnvioNuevoEvent event) {
        log.info("[LISTENER] Nuevo envío creado → disparando email de confirmación. ID: {}",
                event.getEnvio().getIdEnvio());

        notificationService.notificarCambioEstado(
                event.getEnvio(),
                EstadoEnvio.PENDIENTE);
    }
}