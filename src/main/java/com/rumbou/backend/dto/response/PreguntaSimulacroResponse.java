package com.rumbou.backend.dto.response;

import java.util.List;

// Deliberadamente NO incluye claveCorrecta ni explicacion mientras el
// simulacro esta en curso: el usuario no debe poder ver la respuesta.
public record PreguntaSimulacroResponse(
        Long respuestaUsuarioId,
        Long preguntaId,
        String enunciado,
        List<String> alternativas
) {
}
