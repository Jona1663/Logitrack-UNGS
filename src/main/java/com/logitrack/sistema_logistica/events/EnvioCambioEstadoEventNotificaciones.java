package com.logitrack.sistema_logistica.events;

import org.springframework.context.ApplicationEvent;

import com.logitrack.sistema_logistica.model.Envio;

public class EnvioCambioEstadoEventNotificaciones extends ApplicationEvent {

    private final Envio envio;

    public EnvioCambioEstadoEventNotificaciones(Object source, Envio envio) {
        super(source);
        this.envio = envio;
    }

    public Envio getEnvio() {
        return envio;
    }
}