package com.rumbou.backend.examen.dto;

import jakarta.validation.constraints.NotNull;

public record ResponderPreguntaRequest(
        @NotNull Long preguntaId,
        // null significa que el usuario dejo la pregunta en blanco.
        Integer alternativaMarcada
) {
}
