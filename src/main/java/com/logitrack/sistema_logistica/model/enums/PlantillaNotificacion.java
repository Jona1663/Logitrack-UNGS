package com.logitrack.sistema_logistica.model.enums;

public enum PlantillaNotificacion {
//Estados de envio
    PENDIENTE(
        "Tu envío está registrado",
        "Hola {cliente}, tu envío #{idEnvio} ha sido registrado exitosamente y está en estado: Pendiente."
    ),
    EN_TRANSITO(
        "Tu envío está en camino",
        "Hola {cliente}, te avisamos que tu envío #{idEnvio} ahora se encuentra En Tránsito hacia su destino."
    ),
    EN_PUNTO_DE_RECOLECCION(
        "El transportista llegó al punto de carga",
        "Hola {cliente}, el transportista del envío #{idEnvio} llegó al punto de recolección y está cargando la mercadería."
    ),
    EN_REPARTO(
        "Tu envío está siendo entregado",
        "Hola {cliente}, tu envío #{idEnvio} está en reparto y se encuentra en camino al destino final."
    ),
    ENTREGADO(
        "Tu envío fue entregado",
        "Hola {cliente}, el envío #{idEnvio} fue entregado exitosamente. ¡Gracias!"
    ),
    CANCELADO(
        "Tu envío fue cancelado",
        "Hola {cliente}, el envío #{idEnvio} ha sido cancelado. Contactate con nosotros para más información."
    );

    private final String asunto;
    private final String cuerpo;

    PlantillaNotificacion(String asunto, String cuerpo) {
        this.asunto = asunto;
        this.cuerpo = cuerpo;
    }

    public String getAsunto() {
        return asunto;
    }


    //Reemplaza los placeholders {cliente} e {idEnvio} con los valores reales.
    public String getCuerpo(String cliente, String idEnvio) {
        return cuerpo
            .replace("{cliente}", cliente)
            .replace("{idEnvio}", idEnvio);
    }
}