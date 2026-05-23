package com.logitrack.sistema_logistica.service;

public interface NotificationService {
    void enviarNotificacion(String destinatario, String asunto, String mensaje);
}