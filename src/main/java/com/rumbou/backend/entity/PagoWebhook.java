package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// Notificacion de Mercado Pago ya procesada. paymentId es la llave de
// idempotencia: un reenvio de MP se detecta y no se procesa dos veces.
@Entity
@Table(name = "pagos_webhook")
public class PagoWebhook extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long paymentId;

    @Column(nullable = false)
    private String mercadoPagoPreapprovalId;

    private String externalReference;

    @Column(nullable = false)
    private LocalDateTime recibidoEn;

    public PagoWebhook() {
    }

    public PagoWebhook(Long paymentId, String mercadoPagoPreapprovalId, String externalReference) {
        this.paymentId = paymentId;
        this.mercadoPagoPreapprovalId = mercadoPagoPreapprovalId;
        this.externalReference = externalReference;
        this.recibidoEn = LocalDateTime.now();
    }

}