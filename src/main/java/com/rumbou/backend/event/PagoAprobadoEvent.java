package com.rumbou.backend.event;

// Lo publica WebhookService al confirmarse un pago; lo escuchan la activacion del plan y el correo.
public record PagoAprobadoEvent(
        Long paymentId,                   // id de pago de MP (para idempotencia)
        String mercadoPagoPreapprovalId,  // vincula el evento con la suscripcion pendiente
        String externalReference          // referencia nuestra (ej: "usuario-7")
) {
}