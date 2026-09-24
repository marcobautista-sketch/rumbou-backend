package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.WebhookNotificationRequest;

public interface WebhookService {

    void procesar(WebhookNotificationRequest notificacion, String xSignature, String xRequestId);
}
