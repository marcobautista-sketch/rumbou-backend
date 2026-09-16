package com.rumbou.backend.suscripcion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

class SuscripcionVencimientoJobTest {

    private SuscripcionRepository suscripcionRepository;
    private SuscripcionVencimientoJob job;

    @BeforeEach
    void setUp() {
        suscripcionRepository = mock(SuscripcionRepository.class);
        job = new SuscripcionVencimientoJob(suscripcionRepository);
    }

    @Test
    void marcaComoVencidasLasActivasConFechaPasada() {
        Suscripcion vencida = suscripcion(EstadoSuscripcion.ACTIVA, LocalDate.now().minusDays(1));
        Suscripcion vigente = suscripcion(EstadoSuscripcion.ACTIVA, LocalDate.now().plusDays(15));

        when(suscripcionRepository.findByEstadoAndFechaFinBefore(
                eq(EstadoSuscripcion.ACTIVA), any(LocalDate.class)))
                .thenReturn(List.of(vencida));

        job.marcarSuscripcionesVencidas();

        assertThat(vencida.getEstado()).isEqualTo(EstadoSuscripcion.VENCIDA);
        assertThat(vigente.getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
    }

    @Test
    void noMarcaNadaSiNoHaySuscripcionesVencidas() {
        when(suscripcionRepository.findByEstadoAndFechaFinBefore(
                eq(EstadoSuscripcion.ACTIVA), any(LocalDate.class)))
                .thenReturn(List.of());

        job.marcarSuscripcionesVencidas();
    }

    private Suscripcion suscripcion(EstadoSuscripcion estado, LocalDate fechaFin) {
        Suscripcion s = new Suscripcion();
        s.setEstado(estado);
        s.setFechaFin(fechaFin);
        return s;
    }
}