package com.rumbou.backend.client.gemini;

import java.util.List;

// Contrato con Gemini, no con la API REST del proyecto (por eso no vive en dto/).
public record PreguntaGeneradaDto(
        String enunciado,
        List<String> alternativas,
        int claveCorrecta,
        String explicacion
) {
}
