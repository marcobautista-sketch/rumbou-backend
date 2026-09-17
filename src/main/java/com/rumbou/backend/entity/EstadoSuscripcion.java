package com.rumbou.backend.entity;

public enum EstadoSuscripcion {
    PENDIENTE,   // se pago pero Mercado Pago aun no confirma
    ACTIVA,      // plan vigente
    VENCIDA,     // se acabo la fechaFin
    CANCELADA    // el usuario o el negocio la cancelo
}