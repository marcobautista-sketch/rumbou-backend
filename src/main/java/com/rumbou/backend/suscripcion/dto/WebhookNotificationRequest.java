package com.rumbou.backend.suscripcion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que Mercado Pago envia al webhook (version simplificada para el curso;
// en produccion se mapea la notificacion real de MP a estos campos).
public record WebhookNotificationRequest(
        @NotBlank String action,
        @NotNull Long paymentId,
        @NotBlank String mercadoPagoPreapprovalId,
        String externalReference
) {
}