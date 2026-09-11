package com.rumbou.backend.contenido.dto;

import com.rumbou.backend.contenido.Dificultad;

import java.util.List;

// Sin claveCorrecta ni explicacion, igual que PreguntaSimulacroResponse en examen/.
public record PreguntaResponse(
        Long id,
        Long temaId,
        String temaNombre,
        String enunciado,
        List<String> alternativas,
        Dificultad dificultad
) {
}
