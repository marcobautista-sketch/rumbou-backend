package com.rumbou.backend.dto.response;

import com.rumbou.backend.entity.Dificultad;
import java.util.List;

// Incluye clave, explicacion y pasos, a diferencia de PreguntaResponse.
public record PreguntaAdminResponse(
        Long id,
        Long temaId,
        String temaNombre,
        String enunciado,
        List<String> alternativas,
        int claveCorrecta,
        String explicacion,
        Dificultad dificultad,
        boolean aprobada,
        PresentacionResponse presentacion,
        List<PasoResolucionResponse> pasos
) {
}
