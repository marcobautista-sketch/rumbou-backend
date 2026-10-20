package com.rumbou.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ResponderDesafioRequest(
        @NotNull @Min(0) @Max(4) Integer alternativaMarcada
) {
}
