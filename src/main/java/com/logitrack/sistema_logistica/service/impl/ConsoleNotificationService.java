package com.logitrack.sistema_logistica.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.logitrack.sistema_logistica.service.NotificationService;

@Service
public class ConsoleNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void enviarNotificacion(String destinatario, String asunto, String mensaje) {
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║            NOTIFICACION SIMULADA                 ║");
        log.info("╠══════════════════════════════════════════════════╣");
        log.info("║ Para:    {}", destinatario);
        log.info("║ Asunto:  {}", asunto);
        log.info("║ Mensaje: {}", mensaje);
        log.info("║ Hora:    {}", LocalDateTime.now().format(FORMATTER));
        log.info("╚══════════════════════════════════════════════════╝");
    }
}