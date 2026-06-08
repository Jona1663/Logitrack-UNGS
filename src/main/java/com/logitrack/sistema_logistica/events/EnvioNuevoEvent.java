package com.logitrack.sistema_logistica.events;

import org.springframework.context.ApplicationEvent;

import com.logitrack.sistema_logistica.model.Envio;

import lombok.Getter;

/**
 * CREAR — events/EnvioCreatedEvent.java
 *
 * Evento dedicado exclusivamente a la creación de un nuevo envío.
 *
 * Por qué un evento propio y no reutilizar EnvioCambioEstadoEvent con PENDIENTE:
 *   - La creación no es un "cambio de estado" — es una acción diferente.
 *   - Semánticamente más claro: quien lee el código sabe que este evento
 *     solo se publica cuando nace un envío nuevo.
 *   - Permite agregar datos específicos de creación en el futuro
 *     (ej: usuario que creó el envío) sin tocar el evento de cambio de estado.
 *   - El listener de cambio de estado no necesita filtrarlo.
 */
@Getter
public class EnvioNuevoEvent extends ApplicationEvent {

    private final Envio envio;

    public EnvioNuevoEvent(Object source, Envio envio) {
        super(source);
        this.envio = envio;
    }
}
