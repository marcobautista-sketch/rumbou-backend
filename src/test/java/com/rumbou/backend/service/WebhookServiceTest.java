package com.rumbou.backend.service;

import com.rumbou.backend.client.mercadopago.WebhookSignatureValidator;
import com.rumbou.backend.dto.request.WebhookNotificationRequest;
import com.rumbou.backend.entity.PagoWebhook;
import com.rumbou.backend.event.PagoAprobadoEvent;
import com.rumbou.backend.repository.PagoWebhookRepository;
import com.rumbou.backend.service.impl.WebhookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookServiceTest {

    private PagoWebhookRepository pagoWebhookRepository;
    private ApplicationEventPublisher eventPublisher;
    private WebhookServiceImpl webhookService;

    @BeforeEach
    void setUp() {
        pagoWebhookRepository = mock(PagoWebhookRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        webhookService = new WebhookServiceImpl(pagoWebhookRepository, eventPublisher, new WebhookSignatureValidator(""));
    }

    @Test
    void ignoraAccionesQueNoSonDePagoAprobado() {
        WebhookNotificationRequest notificacion =
                new WebhookNotificationRequest("payment.pending", 1L, "preapp-1", "usuario-1");

        webhookService.procesar(notificacion, null, null);

        verify(pagoWebhookRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void guardaElReciboYPublicaElEventoEnUnPagoNuevo() {
        when(pagoWebhookRepository.existsByPaymentId(500L)).thenReturn(false);
        WebhookNotificationRequest notificacion =
                new WebhookNotificationRequest("payment.approved", 500L, "preapp-500", "usuario-5");

        webhookService.procesar(notificacion, null, null);

        verify(pagoWebhookRepository).save(any(PagoWebhook.class));
        verify(eventPublisher).publishEvent(any(PagoAprobadoEvent.class));
    }

    @Test
    void noReprocesaUnPaymentIdDuplicado() {
        when(pagoWebhookRepository.existsByPaymentId(500L)).thenReturn(true);
        WebhookNotificationRequest notificacion =
                new WebhookNotificationRequest("payment.approved", 500L, "preapp-500", "usuario-5");

        webhookService.procesar(notificacion, null, null);

        verify(pagoWebhookRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}