package com.rumbou.backend.suscripcion;

import com.rumbou.backend.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// El "recibo" de una notificacion de Mercado Pago ya procesada.
// paymentId unico = la llave de idempotencia: si MP reenvia la misma
// notificacion, existsByPaymentId() la detecta y no se procesa dos veces.
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

    // getters y setters para los 4 campos (BaseEntity ya da el id)
}