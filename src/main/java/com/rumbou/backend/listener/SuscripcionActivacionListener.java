package com.rumbou.backend.listener;

import com.rumbou.backend.event.PagoAprobadoEvent;
import com.rumbou.backend.service.SuscripcionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// AFTER_COMMIT: el pago ya esta en BD. REQUIRES_NEW: la transaccion del webhook
// ya se cerro, hay que abrir otra para guardar la activacion.
@Component
public class SuscripcionActivacionListener {

    private final SuscripcionService suscripcionService;

    public SuscripcionActivacionListener(SuscripcionService suscripcionService) {
        this.suscripcionService = suscripcionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alRecibirPagoAprobado(PagoAprobadoEvent evento) {
        suscripcionService.activar(evento);
    }
}