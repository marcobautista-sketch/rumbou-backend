package com.rumbou.backend.dto.response;

import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.OrigenPregunta;
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
        OrigenPregunta origen,
        boolean aprobada,
        PresentacionResponse presentacion,
        List<PasoResolucionResponse> pasos
) {
}
