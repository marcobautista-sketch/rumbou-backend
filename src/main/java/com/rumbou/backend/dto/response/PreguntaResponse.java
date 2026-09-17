package com.rumbou.backend.dto.response;

import com.rumbou.backend.entity.Dificultad;
import java.util.List;

// Sin claveCorrecta ni explicacion, igual que PreguntaSimulacroResponse.
public record PreguntaResponse(
        Long id,
        Long temaId,
        String temaNombre,
        String enunciado,
        List<String> alternativas,
        Dificultad dificultad
) {
}
