package com.rumbou.backend.dto.response;

// Lo que el postulante necesita para elegir su objetivo: que carrera, en que
// universidad y area, y con cuanto puntaje entro el ultimo ingresante.
public record OfertaAcademicaResponse(
        Long id,
        String universidad,
        String area,
        String areaNombre,
        String carrera,
        String facultad,
        String procesoAdmision,
        double puntajeUltimoIngresante,
        int vacantes
) {
}
