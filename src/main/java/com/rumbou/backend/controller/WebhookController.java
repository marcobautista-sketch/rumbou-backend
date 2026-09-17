package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.WebhookNotificationRequest;
import com.rumbou.backend.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/mercadopago")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<Void> recibir(@Valid @RequestBody WebhookNotificationRequest notificacion) {
        webhookService.procesar(notificacion);
        return ResponseEntity.ok().build();
    }
}