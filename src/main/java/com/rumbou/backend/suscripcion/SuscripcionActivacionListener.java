package com.rumbou.backend.suscripcion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// AFTER_COMMIT: el webhook ya persistio su pago en BD; solo ahi activamos.
// REQUIRES_NEW: cuando AFTER_COMMIT se ejecuta, la transaccion original ya
// se cerro (la transaccion del webhook). Para guardar la activacion,
// hay que abrir una nueva.
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