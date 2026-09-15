package com.rumbou.backend.correo;

import com.rumbou.backend.auth.UsuarioRegistradoEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

// AFTER_COMMIT: el usuario ya esta persistido en BD. @Async: enviar el correo
// demora (SMTP) y no debe bloquear la respuesta del registro.
@Component
public class RegistroConfirmacionListener {

    private final EmailService emailService;

    public RegistroConfirmacionListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarUsuario(UsuarioRegistradoEvent evento) {
        emailService.enviarPlantilla(
                evento.email(),
                "Bienvenido a RumboU",
                "registro-confirmacion",
                Map.of("nombre", evento.nombre(), "email", evento.email()));
    }
}