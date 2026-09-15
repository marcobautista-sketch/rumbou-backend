package com.rumbou.backend.correo;

import com.rumbou.backend.auth.PasswordResetRequestedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class RecuperacionContrasenaCorreoListener {

    private static final String URL_BASE = "https://rumbou.edu.pe/recuperar";

    private final EmailService emailService;

    public RecuperacionContrasenaCorreoListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alPedirRecuperacion(PasswordResetRequestedEvent evento) {
        emailService.enviarPlantilla(
                evento.email(),
                "Recupera tu contrasena",
                "recuperacion-contrasena",
                Map.of("nombre", evento.nombre(),
                        "urlCambio", URL_BASE + "?token=" + evento.token()));
    }
}