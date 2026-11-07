package com.rumbou.backend.service;

import com.rumbou.backend.entity.Usuario;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

public final class SemanaActividad {

    private SemanaActividad() {
    }

    public static LocalDate hoy(Clock reloj) {
        return LocalDate.now(reloj.withZone(ZoneId.of("America/Lima")));
    }

    public static LocalDate inicio(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static int xpVigente(Usuario usuario, LocalDate semanaInicio) {
        LocalDate ultimaActividad = usuario.getLastActivityDate();
        return ultimaActividad == null || ultimaActividad.isBefore(semanaInicio)
                ? 0 : usuario.getXpSemanal();
    }
}
