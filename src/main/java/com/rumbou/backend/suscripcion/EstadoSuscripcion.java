package com.rumbou.backend.suscripcion;

public enum EstadoSuscripcion {
    PENDIENTE,   // se pago pero Mercado Pago aun no confirma
    ACTIVA,      // plan vigente
    VENCIDA,     // se acabo la fechaFin
    CANCELADA    // el usuario o el negocio la cancelo
}