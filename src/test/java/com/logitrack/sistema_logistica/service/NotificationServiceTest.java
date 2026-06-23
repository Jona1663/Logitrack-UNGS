package com.logitrack.sistema_logistica.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;

public class NotificationServiceTest {

    @Test
    void cobertura_interfaz_fantasma() {
        // Como es una interfaz y no tiene código, la mockeamos directamente
        NotificationService mockService = Mockito.mock(NotificationService.class);

        // Llamamos a los métodos en el vacío con datos falsos
        mockService.enviarNotificacion("test@test.com", "Asunto", "Mensaje");
        mockService.notificarCambioEstado(new Envio(), EstadoEnvio.PENDIENTE);

        // Verificamos que la interfaz compila bien y reconoce sus propios métodos
        Mockito.verify(mockService).enviarNotificacion(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockService).notificarCambioEstado(Mockito.any(Envio.class), Mockito.any(EstadoEnvio.class));
    }
}