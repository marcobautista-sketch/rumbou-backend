package com.rumbou.backend.dto.request;

import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.OrigenPregunta;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Actualizacion completa, no parcial: reemplaza todos los campos editables.
public record UpdatePreguntaRequest(
        @NotNull Long temaId,

        @NotBlank String enunciado,

        @NotNull
        @Size(min = 5, max = 5, message = "Debe haber exactamente 5 alternativas")
        List<@NotBlank String> alternativas,

        @Min(0) @Max(4) int claveCorrecta,

        String explicacion,

        @NotNull Dificultad dificultad,

        @NotNull OrigenPregunta origen,

        boolean aprobada
) {
}
