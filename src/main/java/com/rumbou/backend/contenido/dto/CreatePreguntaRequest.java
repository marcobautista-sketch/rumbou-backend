package com.rumbou.backend.contenido.dto;

import com.rumbou.backend.contenido.Dificultad;
import com.rumbou.backend.contenido.OrigenPregunta;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePreguntaRequest(
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
