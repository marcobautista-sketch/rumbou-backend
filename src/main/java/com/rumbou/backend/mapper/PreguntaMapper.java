package com.rumbou.backend.mapper;

import com.rumbou.backend.dto.response.PreguntaAdminResponse;
import com.rumbou.backend.dto.response.PreguntaResponse;
import com.rumbou.backend.entity.Pregunta;

public final class PreguntaMapper {

    private PreguntaMapper() {
    }

    // Vista del postulante: sin clave ni explicacion.
    public static PreguntaResponse toResponse(Pregunta pregunta) {
        return new PreguntaResponse(
                pregunta.getId(),
                pregunta.getTema().getId(),
                pregunta.getTema().getNombre(),
                pregunta.getEnunciado(),
                pregunta.getAlternativas(),
                pregunta.getDificultad());
    }

    public static PreguntaAdminResponse toAdminResponse(Pregunta pregunta) {
        return new PreguntaAdminResponse(
                pregunta.getId(),
                pregunta.getTema().getId(),
                pregunta.getTema().getNombre(),
                pregunta.getEnunciado(),
                pregunta.getAlternativas(),
                pregunta.getClaveCorrecta(),
                pregunta.getExplicacion(),
                pregunta.getDificultad(),
                pregunta.getOrigen(),
                pregunta.isAprobada());
    }
}
