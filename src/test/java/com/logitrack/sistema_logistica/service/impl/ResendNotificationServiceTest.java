package com.logitrack.sistema_logistica.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;

@ExtendWith(MockitoExtension.class)
public class ResendNotificationServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private ResendNotificationService resendNotificationService;

    @BeforeEach
    void setUp() {
        // Inyectamos las configuraciones para que el servicio no tire NullPointerException
        ReflectionTestUtils.setField(resendNotificationService, "apiKey", "fake_key_123");
        ReflectionTestUtils.setField(resendNotificationService, "fromAddress", "test@logitrack.com");
        ReflectionTestUtils.setField(resendNotificationService, "fromName", "Sistema Logitrack");
        ReflectionTestUtils.setField(resendNotificationService, "enabled", true);
        ReflectionTestUtils.setField(resendNotificationService, "useRealRecipient", false);
        ReflectionTestUtils.setField(resendNotificationService, "toOverride", "tester@test.com");
    }

    // ==========================================
    // TESTS: enviarNotificacion
    // ==========================================

    @Test
    void enviarNotificacion_CuandoEstaDeshabilitado_NoHaceNada() {
        ReflectionTestUtils.setField(resendNotificationService, "enabled", false);
        resendNotificationService.enviarNotificacion("destino@test.com", "Alerta", "Cuidado");
    }

    @Test
    void enviarNotificacion_FuerzaCatchResendException() {
        // Falla a propósito porque la ApiKey es falsa
        resendNotificationService.enviarNotificacion("destino@test.com", "Alerta", "Cuidado");
    }

    // ==========================================
    // TESTS: notificarCambioEstado
    // ==========================================

    @Test
    void notificarCambioEstado_CuandoEstaDeshabilitado_NoHaceNada() {
        ReflectionTestUtils.setField(resendNotificationService, "enabled", false);
        Envio envio = new Envio();
        envio.setIdEnvio("LT-999");
        resendNotificationService.notificarCambioEstado(envio, EstadoEnvio.PENDIENTE);
    }

    @Test
    void notificarCambioEstado_CuandoNoHayDestinatario_SaleDelMetodo() {
        ReflectionTestUtils.setField(resendNotificationService, "toOverride", "");
        Envio envio = new Envio();
        envio.setIdEnvio("LT-999");
        resendNotificationService.notificarCambioEstado(envio, EstadoEnvio.PENDIENTE);
    }

    @Test
    void notificarCambioEstado_RecorreTodosLosEstados() {
        // Simulamos la respuesta de Thymeleaf para que no rompa
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Mock</html>");

        Envio envio = new Envio();
        envio.setIdEnvio("LT-123");
        // No seteamos kilos, ni origen, ni destino. El servicio va a usar los valores "—" y "Cliente".

        // Evaluamos TODOS los estados para cubrir los switch completos
        resendNotificationService.notificarCambioEstado(envio, EstadoEnvio.PENDIENTE);
        resendNotificationService.notificarCambioEstado(envio, EstadoEnvio.EN_TRANSITO);
        resendNotificationService.notificarCambioEstado(envio, EstadoEnvio.EN_PUNTO_DE_RECOLECCION);
        resendNotificationService.notificarCambioEstado(envio, EstadoEnvio.EN_REPARTO);
        resendNotificationService.notificarCambioEstado(envio, EstadoEnvio.ENTREGADO);
        resendNotificationService.notificarCambioEstado(envio, EstadoEnvio.CANCELADO);
    }

    @Test
    void notificarCambioEstado_FuerzaCatchGenerico() {
        // Forzamos un error en la plantilla para disparar el catch Exception
        when(templateEngine.process(anyString(), any(Context.class))).thenThrow(new RuntimeException("Fallo interno"));

        Envio envio = new Envio();
        envio.setIdEnvio("LT-ERR");
        
        resendNotificationService.notificarCambioEstado(envio, EstadoEnvio.PENDIENTE);
    }

    // ==========================================
    // TESTS: CONFIGURACIONES EXTRAS
    // ==========================================

    @Test
    void resolverDestinatario_UsaRealRecipientEnTrue() {
        ReflectionTestUtils.setField(resendNotificationService, "useRealRecipient", true);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Mock</html>");

        Envio envio = new Envio();
        envio.setIdEnvio("LT-DEST");
        
        resendNotificationService.notificarCambioEstado(envio, EstadoEnvio.ENTREGADO);
    }
}