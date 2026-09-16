package com.rumbou.backend.suscripcion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// Corre una vez al dia a las 3 a.m. y marca como VENCIDA las suscripciones
// ACTIVA cuya fechaFin ya paso (seccion 9.5 del documento de decisiones).
@Component
public class SuscripcionVencimientoJob {

    private static final Logger log = LoggerFactory.getLogger(SuscripcionVencimientoJob.class);

    private final SuscripcionRepository suscripcionRepository;

    public SuscripcionVencimientoJob(SuscripcionRepository suscripcionRepository) {
        this.suscripcionRepository = suscripcionRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void marcarSuscripcionesVencidas() {
        LocalDate hoy = LocalDate.now();
        List<Suscripcion> vencidas = suscripcionRepository
                .findByEstadoAndFechaFinBefore(EstadoSuscripcion.ACTIVA, hoy);
        if (vencidas.isEmpty()) {
            return;
        }
        vencidas.forEach(s -> s.setEstado(EstadoSuscripcion.VENCIDA));
        log.info("{} suscripciones marcadas como VENCIDA", vencidas.size());
    }
}