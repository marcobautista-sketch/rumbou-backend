package com.rumbou.backend.suscripcion;

import com.rumbou.backend.suscripcion.dto.WebhookNotificationRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private static final String ACCION_PAGO_APROBADO = "payment.approved";

    private final PagoWebhookRepository pagoWebhookRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WebhookService(PagoWebhookRepository pagoWebhookRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.pagoWebhookRepository = pagoWebhookRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void procesar(WebhookNotificationRequest notificacion) {
        // MP envia muchas acciones; solo nos interesa la aprobacion.
        if (!ACCION_PAGO_APROBADO.equals(notificacion.action())) {
            return;
        }

        // IDEMPOTENCIA: si este pago ya se proceso, ignoramos el duplicado.
        if (pagoWebhookRepository.existsByPaymentId(notificacion.paymentId())) {
            return;
        }

        PagoWebhook recibo = new PagoWebhook(
                notificacion.paymentId(),
                notificacion.mercadoPagoPreapprovalId(),
                notificacion.externalReference());
        pagoWebhookRepository.save(recibo);

        // El "grito" que escucha el listener de la Pieza 5 (despues del commit).
        eventPublisher.publishEvent(new PagoAprobadoEvent(
                notificacion.paymentId(),
                notificacion.mercadoPagoPreapprovalId(),
                notificacion.externalReference()));
    }
}