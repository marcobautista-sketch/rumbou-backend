package com.rumbou.backend.scheduler;

import com.rumbou.backend.entity.EstadoSuscripcion;
import com.rumbou.backend.entity.Suscripcion;
import com.rumbou.backend.repository.SuscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// Cada dia a las 3 a.m. marca VENCIDA toda suscripcion ACTIVA cuya fechaFin ya paso.
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