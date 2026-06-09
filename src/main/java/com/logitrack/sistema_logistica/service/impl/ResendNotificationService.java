package com.logitrack.sistema_logistica.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.service.NotificationService;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("resendNotificationService")
@RequiredArgsConstructor
public class ResendNotificationService implements NotificationService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from}")
    private String fromAddress;

    @Value("${resend.from-name}")
    private String fromName;

    @Value("${resend.enabled:true}")
    private boolean enabled;

    @Value("${resend.use-real-recipient:false}")
    private boolean useRealRecipient;

    @Value("${resend.to-override:federicogrande2013@gmail.com}")
    private String toOverride;

    @Qualifier("emailTemplateEngine")
    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // 1 Notificación al supervisor por incidencia 

    @Async("mailTaskExecutor")
    @Override
    public void enviarNotificacion(String destinatario, String asunto, String mensaje) {
        if (!enabled) {
            log.info("[RESEND] Deshabilitado. Asunto: {}", asunto);
            return;
        }
        try {
            Resend resend = new Resend(apiKey);

            String html = "<div style='font-family:Arial,sans-serif;font-size:14px;line-height:1.6;'>"
                + mensaje.replace("\n", "<br/>")
                + "</div>";

            CreateEmailOptions email = CreateEmailOptions.builder()
                .from(fromName + " <" + fromAddress + ">")
                .to(destinatario)
                .subject(asunto)
                .html(html)
                .build();

            CreateEmailResponse response = resend.emails().send(email);
            log.info("[RESEND] Notificación enviada → id={} | para={}", response.getId(), destinatario);

        } catch (ResendException e) {
            log.error("[RESEND] Error al enviar notificación a {}: {}", destinatario, e.getMessage(), e);
        } catch (Exception e) {
            log.error("[RESEND] Falla inesperada al notificar a {}: {}", destinatario, e.getMessage(), e);
        }
    }

    // 2 Notificación al cliente por estado del envío 

    @Async("mailTaskExecutor")
    @Override
    public void notificarCambioEstado(Envio envio, EstadoEnvio nuevoEstado) {
        if (!enabled) {
            log.info("[RESEND] Deshabilitado. Envío: {}", envio.getIdEnvio());
            return;
        }

        String destinatario = resolverDestinatario(envio);
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("[RESEND] Sin destinatario para envío {}. Omitido.", envio.getIdEnvio());
            return;
        }

        try {
            // Cada estado usa su propia plantilla HTML
            String plantilla = elegirPlantilla(nuevoEstado);
            String html      = renderizarPlantilla(plantilla, envio, nuevoEstado);
            String asunto    = elegirAsunto(envio, nuevoEstado);

            Resend resend = new Resend(apiKey);

            CreateEmailOptions email = CreateEmailOptions.builder()
                .from(fromName + " <" + fromAddress + ">")
                .to(destinatario)
                .subject(asunto)
                .html(html)
                .build();

            CreateEmailResponse response = resend.emails().send(email);
            log.info("[RESEND] OK → resend_id={} | para={} | envío={} | estado={} | plantilla={}",
                response.getId(), destinatario, envio.getIdEnvio(), nuevoEstado, plantilla);

        } catch (ResendException e) {
            log.error("[RESEND] Error Resend para envío {}: {}", envio.getIdEnvio(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("[RESEND] Falla inesperada para envío {}: {}", envio.getIdEnvio(), e.getMessage(), e);
        }
    }


    private String elegirPlantilla(EstadoEnvio estado) {
        return switch (estado) {
            case PENDIENTE -> "envio-creado";
            case CANCELADO -> "envio-cancelado";
            default        -> "cambio-estado";
        };
    }

    // Asunto del email según el estado.
    private String elegirAsunto(Envio envio, EstadoEnvio estado) {
        return switch (estado) {
            case PENDIENTE -> String.format("Logitrack · Envío %s registrado correctamente", envio.getIdEnvio());
            case CANCELADO -> String.format("Logitrack · Envío %s cancelado", envio.getIdEnvio());
            default        -> String.format("Logitrack · Envío %s: %s", envio.getIdEnvio(), labelEstado(estado));
        };
    }

    private String renderizarPlantilla(String nombrePlantilla, Envio envio, EstadoEnvio nuevoEstado) {
        Establecimiento origen  = envio.getOrigen();
        Establecimiento destino = envio.getDestino();

        String origenStr  = (origen != null && origen.getNombreLugar() != null) 
                            ? origen.getNombreLugar() : "—";
        String destinoStr = (destino != null && destino.getNombreLugar() != null) 
                            ? destino.getNombreLugar() : "—";

        // Nombre del cliente a partir de la empresa asociada al origen
        String nombreCliente = "Cliente";
        if (origen != null && origen.getEmpresa() != null) {
            nombreCliente = origen.getEmpresa().getRazonSocial();}

        Context ctx = new Context(Locale.forLanguageTag("es-AR"));
        ctx.setVariable("nombreCliente",   nombreCliente);
        ctx.setVariable("idSeguimiento",   envio.getIdEnvio());
        ctx.setVariable("origen",          origenStr);
        ctx.setVariable("destino",         destinoStr);
        ctx.setVariable("estadoLabel",     labelEstado(nuevoEstado));
        ctx.setVariable("toneladas",     envio.getKgOrigen());
        ctx.setVariable("estadoCssClass",  cssClass(nuevoEstado));
        ctx.setVariable("fechaHoraEvento", LocalDateTime.now().format(FORMATTER));
        ctx.setVariable("tipoGrano",       envio.getTipoGrano() != null
                                            ? envio.getTipoGrano().name() : "—");
        ctx.setVariable("comentarios",     envio.getComentarios() != null
                                            ? envio.getComentarios() : "");

        return templateEngine.process(nombrePlantilla, ctx);
    }

    private String resolverDestinatario(Envio envio) {
        if (useRealRecipient) {
            return toOverride;
        }
        return toOverride;
    }

    private String labelEstado(EstadoEnvio estado) {
        return switch (estado) {
            case PENDIENTE               -> "Pendiente";
            case EN_TRANSITO             -> "En Tránsito";
            case EN_PUNTO_DE_RECOLECCION -> "En punto de recolección";
            case EN_REPARTO              -> "En Reparto";
            case ENTREGADO               -> "Entregado";
            case CANCELADO               -> "Cancelado";
        };
    }

    private String cssClass(EstadoEnvio estado) {
        return switch (estado) {
            case PENDIENTE               -> "badge--pendiente";
            case EN_TRANSITO             -> "badge--transito";
            case EN_PUNTO_DE_RECOLECCION -> "badge--recoleccion";
            case EN_REPARTO              -> "badge--reparto";
            case ENTREGADO               -> "badge--entregado";
            case CANCELADO               -> "badge--cancelado";
        };
    }
}