package com.rumbou.backend.listener;

import com.rumbou.backend.entity.Suscripcion;
import com.rumbou.backend.event.PagoAprobadoEvent;
import com.rumbou.backend.repository.SuscripcionRepository;
import com.rumbou.backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Optional;

@Component
public class PagoAprobadoCorreoListener {

    private static final Logger log = LoggerFactory.getLogger(PagoAprobadoCorreoListener.class);

    private final EmailService emailService;
    private final SuscripcionRepository suscripcionRepository;

    public PagoAprobadoCorreoListener(EmailService emailService,
                                      SuscripcionRepository suscripcionRepository) {
        this.emailService = emailService;
        this.suscripcionRepository = suscripcionRepository;
    }

    // REQUIRES_NEW: tras AFTER_COMMIT no hay transaccion activa y aqui se carga
    // el usuario LAZY de la suscripcion.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alAprobarPago(PagoAprobadoEvent evento) {
        Optional<Suscripcion> suscripcion = suscripcionRepository
                .findFirstByMercadoPagoPreapprovalId(evento.mercadoPagoPreapprovalId());
        if (suscripcion.isEmpty()) {
            log.warn("Pago aprobado pero no existe la suscripcion con preaprobacion {}",
                    evento.mercadoPagoPreapprovalId());
            return;
        }

        Suscripcion s = suscripcion.get();
        emailService.enviarPlantilla(
                s.getUsuario().getEmail(),
                "Tu plan PRO esta activo",
                "pago-aprobado",
                Map.of("nombre", s.getUsuario().getNombre()));
    }
}