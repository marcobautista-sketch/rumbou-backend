package com.rumbou.backend.service;

import com.rumbou.backend.client.mercadopago.WebhookSignatureValidator;
import com.rumbou.backend.dto.request.WebhookNotificationRequest;
import com.rumbou.backend.entity.PagoWebhook;
import com.rumbou.backend.event.PagoAprobadoEvent;
import com.rumbou.backend.repository.PagoWebhookRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private static final String ACCION_PAGO_APROBADO = "payment.approved";

    private final PagoWebhookRepository pagoWebhookRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final WebhookSignatureValidator signatureValidator;

    public WebhookService(PagoWebhookRepository pagoWebhookRepository,
                          ApplicationEventPublisher eventPublisher,
                          WebhookSignatureValidator signatureValidator) {
        this.pagoWebhookRepository = pagoWebhookRepository;
        this.eventPublisher = eventPublisher;
        this.signatureValidator = signatureValidator;
    }

    @Transactional
    public void procesar(WebhookNotificationRequest notificacion, String xSignature, String xRequestId) {
        signatureValidator.validar(xSignature, xRequestId, String.valueOf(notificacion.paymentId()));

        // MP envia muchas acciones; solo nos interesa la aprobacion.
        if (!ACCION_PAGO_APROBADO.equals(notificacion.action())) {
            return;
        }

        // Idempotencia: un pago ya procesado se ignora.
        if (pagoWebhookRepository.existsByPaymentId(notificacion.paymentId())) {
            return;
        }

        PagoWebhook recibo = new PagoWebhook(
                notificacion.paymentId(),
                notificacion.mercadoPagoPreapprovalId(),
                notificacion.externalReference());
        pagoWebhookRepository.save(recibo);

        eventPublisher.publishEvent(new PagoAprobadoEvent(
                notificacion.paymentId(),
                notificacion.mercadoPagoPreapprovalId(),
                notificacion.externalReference()));
    }
}