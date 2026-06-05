package com.logitrack.sistema_logistica.events;

import org.springframework.context.ApplicationEvent;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;

import lombok.Getter;

@Getter
public class EnvioCambioEstadoEvent extends ApplicationEvent {

    private final Envio envio;
    private final EstadoEnvio nuevoEstado;

    public EnvioCambioEstadoEvent(Object source, Envio envio, EstadoEnvio nuevoEstado) {
        super(source);
        this.envio       = envio;        // ← fix: faltaba esta asignación
        this.nuevoEstado = nuevoEstado;  // ← fix: faltaba esta asignación
    }
}