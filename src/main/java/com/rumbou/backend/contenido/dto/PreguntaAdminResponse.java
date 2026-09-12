package com.rumbou.backend.contenido.dto;

import com.rumbou.backend.contenido.Dificultad;
import com.rumbou.backend.contenido.OrigenPregunta;

import java.util.List;

// Incluye clave y explicacion, a diferencia de PreguntaResponse.
public record PreguntaAdminResponse(
        Long id,
        Long temaId,
        String temaNombre,
        String enunciado,
        List<String> alternativas,
        int claveCorrecta,
        String explicacion,
        Dificultad dificultad,
        OrigenPregunta origen,
        boolean aprobada
) {
}
