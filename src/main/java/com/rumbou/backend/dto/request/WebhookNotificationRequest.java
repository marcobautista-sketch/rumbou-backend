package com.rumbou.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Version simplificada de la notificacion de Mercado Pago.
public record WebhookNotificationRequest(
        @NotBlank String action,
        @NotNull Long paymentId,
        @NotBlank String mercadoPagoPreapprovalId,
        String externalReference
) {
}