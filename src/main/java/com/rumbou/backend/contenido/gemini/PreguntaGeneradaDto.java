package com.rumbou.backend.contenido.gemini;

import java.util.List;

// Forma del JSON que le pedimos a Gemini que devuelva para cada pregunta generada.
// No es un DTO de la API REST del proyecto (por eso vive en el subpaquete gemini,
// separado de contenido/dto): es solo el contrato con el modelo de IA.
public record PreguntaGeneradaDto(
        String enunciado,
        List<String> alternativas,
        int claveCorrecta,
        String explicacion
) {
}
