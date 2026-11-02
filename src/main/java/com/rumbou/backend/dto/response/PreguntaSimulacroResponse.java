package com.rumbou.backend.dto.response;

import java.util.List;

// Sin claveCorrecta, explicacion ni pasos: el simulacro esta en curso.
public record PreguntaSimulacroResponse(
        Long respuestaUsuarioId,
        Long preguntaId,
        String enunciado,
        List<String> alternativas,
        PresentacionResponse presentacion
) {
}
