package com.rumbou.backend.client.gemini;

import com.rumbou.backend.entity.Dificultad;
import java.util.List;

// Igual que PreguntaGeneradaDto, con la dificultad que Gemini asigno en la llamada por tema.
public record PreguntaGeneradaConDificultadDto(
        Dificultad dificultad,
        String enunciado,
        List<String> alternativas,
        int claveCorrecta,
        String explicacion
) {
    public PreguntaGeneradaDto sinDificultad() {
        return new PreguntaGeneradaDto(enunciado, alternativas, claveCorrecta, explicacion);
    }
}
