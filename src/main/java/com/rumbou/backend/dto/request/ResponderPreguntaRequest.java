package com.rumbou.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record ResponderPreguntaRequest(
        @NotNull Long preguntaId,
        // null significa que el usuario dejo la pregunta en blanco.
        Integer alternativaMarcada
) {
}
