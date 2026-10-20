package com.rumbou.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Opcional: con la alternativa que marco el postulante, el tutor explica por que lo engano.
public record TutorIaRequest(
        @Min(0) @Max(4) Integer alternativaMarcada
) {
}
