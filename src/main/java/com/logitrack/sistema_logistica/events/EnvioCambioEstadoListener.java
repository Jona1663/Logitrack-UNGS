package com.logitrack.sistema_logistica.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.logitrack.sistema_logistica.model.enums.PlantillaNotificacion;
import com.logitrack.sistema_logistica.service.EmailService;
import com.logitrack.sistema_logistica.service.NotificationService;

@Component
public class EnvioCambioEstadoListener {

    private static final Logger log = LoggerFactory.getLogger(EnvioCambioEstadoListener.class);
    private final NotificationService notificationService;
    private final EmailService emailService;  // ← Agregar

    // Constructor actualizado
    public EnvioCambioEstadoListener(NotificationService notificationService, EmailService emailService) {
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @EventListener
    public void onCambioEstado(EnvioCambioEstadoEvent event) {
        try {
            var envio = event.getEnvio();
            PlantillaNotificacion plantilla = PlantillaNotificacion.valueOf(envio.getEstadoActual().name());
            String cliente = envio.getOrigen().getEmpresa().getRazonSocial();
            String asunto = plantilla.getAsunto();
            String mensaje = plantilla.getCuerpo(cliente, envio.getIdEnvio());

            // 1. Notificación por consola (sigue activa)
            notificationService.enviarNotificacion("cliente-fijo@logitrack.com", asunto, mensaje);

            // 2. Email real al cliente (ahora descomentado y funcional)
            String email = envio.getOrigen().getEmpresa().getEmail();
            if (email != null && !email.isBlank()) {
                emailService.sendEmail(email, asunto, mensaje);
                log.info("Email enviado a {} para envío {}", email, envio.getIdEnvio());
            } else {
                log.warn("El cliente {} no tiene email de contacto registrado", cliente);
            }

        } catch (Exception e) {
            log.warn("No se pudo enviar notificación para envío {}: {}",
                event.getEnvio().getIdEnvio(), e.getMessage());
        }
    }
}