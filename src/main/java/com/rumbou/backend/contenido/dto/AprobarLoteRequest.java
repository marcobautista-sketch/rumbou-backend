package com.rumbou.backend.contenido.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AprobarLoteRequest(
        @NotEmpty(message = "Debe indicar al menos un id de pregunta")
        List<@NotNull Long> ids
) {
}
