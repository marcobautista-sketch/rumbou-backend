package com.rumbou.backend.contenido.gemini;

import com.rumbou.backend.contenido.Dificultad;

import java.util.List;

// Respuesta de la llamada "por tema" (GeminiClient.generarPreguntasPorTema):
// igual que PreguntaGeneradaDto pero cada pregunta trae la dificultad que
// Gemini le asigno, porque en esa llamada se piden las tres de una vez.
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
