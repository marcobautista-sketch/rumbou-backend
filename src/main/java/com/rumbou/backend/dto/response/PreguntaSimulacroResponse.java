package com.rumbou.backend.dto.response;

import java.util.List;

// Sin claveCorrecta ni explicacion: el simulacro esta en curso.
public record PreguntaSimulacroResponse(
        Long respuestaUsuarioId,
        Long preguntaId,
        String enunciado,
        List<String> alternativas
) {
}
