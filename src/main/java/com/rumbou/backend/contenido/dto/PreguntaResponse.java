package com.rumbou.backend.contenido.dto;

import com.rumbou.backend.contenido.Dificultad;

import java.util.List;

// Para el postulante: deliberadamente SIN claveCorrecta ni explicacion,
// igual que examen/dto/PreguntaSimulacroResponse, para no filtrar la
// respuesta correcta antes de que conteste.
public record PreguntaResponse(
        Long id,
        Long temaId,
        String temaNombre,
        String enunciado,
        List<String> alternativas,
        Dificultad dificultad
) {
}
