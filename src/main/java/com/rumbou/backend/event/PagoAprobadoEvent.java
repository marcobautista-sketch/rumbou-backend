package com.rumbou.backend.event;

// Evento que publica el webhook al recibir confirmacion de Mercado Pago.
// Lo escucha SuscripcionActivacionListener (este modulo) para activar el PRO.
// Los campos son record (Java 16+):Spring no los persiste, solo transporta datos entre
// el que publica y el que escucha.
public record PagoAprobadoEvent(
        Long paymentId,                   // id de pago de MP (para idempotencia)
        String mercadoPagoPreapprovalId,  // vincula el evento con la suscripcion pendiente
        String externalReference          // referencia nuestra (ej: "usuario-7")
) {
}