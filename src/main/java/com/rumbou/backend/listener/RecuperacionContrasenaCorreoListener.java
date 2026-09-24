package com.rumbou.backend.listener;

import com.rumbou.backend.event.PasswordResetRequestedEvent;
import com.rumbou.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class RecuperacionContrasenaCorreoListener {

    private final EmailService emailService;
    private final String urlCambio;

    public RecuperacionContrasenaCorreoListener(EmailService emailService,
                                                @Value("${app.frontend.reset-password-url}") String urlCambio) {
        this.emailService = emailService;
        this.urlCambio = urlCambio;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alPedirRecuperacion(PasswordResetRequestedEvent evento) {
        emailService.enviarPlantilla(
                evento.email(),
                "Recupera tu contrasena",
                "recuperacion-contrasena",
                Map.of("nombre", evento.nombre(),
                        "urlCambio", urlCambio + "?token=" + evento.token()));
    }
}