package com.rumbou.backend.dto.response;

// Lo que el postulante necesita para elegir su objetivo: que carrera, en que
// universidad y area, y con cuanto puntaje entro el ultimo ingresante.
// areaId no es un dato decorativo: es lo que el cliente necesita despues para
// iniciar un simulacro de esa area y para pedir su historial de PSP.
public record OfertaAcademicaResponse(
        Long id,
        String universidad,
        Long areaId,
        String area,
        String areaNombre,
        String carrera,
        String facultad,
        String procesoAdmision,
        double puntajeUltimoIngresante,
        int vacantes
) {
}
