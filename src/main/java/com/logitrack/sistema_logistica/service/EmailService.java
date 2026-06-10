package com.logitrack.sistema_logistica.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}