package com.logitrack.sistema_logistica.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.logitrack.sistema_logistica.service.EmailService;

@Primary
@Service
public class MockEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(MockEmailServiceImpl.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void sendEmail(String to, String subject, String body) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║               EMAIL SIMULADO                   ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║ Para:    " + to);
        System.out.println("║ Asunto:  " + subject);
        System.out.println("║ Mensaje: " + body);
        System.out.println("║ Hora:    " + LocalDateTime.now().format(FORMATTER));
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        // Implementación real con JavaMailSender
        // Descomentar cuando haya clientes con email real
        // y AGRAGAR spring-boot-starter-mail al pom.xml
        // ->
        // SimpleMailMessage message = new SimpleMailMessage();
        // message.setTo(to);
        // message.setSubject(subject);
        // message.setText(body);
        // mailSender.send(message);
    }
}