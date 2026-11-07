package com.rumbou.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.text.Normalizer;

public record ParticipacionRankingRequest(
        @NotBlank(message = "El alias es obligatorio")
        @Size(min = 3, max = 20, message = "El alias debe tener entre 3 y 20 caracteres")
        @Pattern(regexp = "[\\p{L}\\p{Nd} ._-]+",
                message = "El alias solo puede contener letras, digitos, espacios, puntos, guiones y guiones bajos")
        String alias
) {
    public ParticipacionRankingRequest {
        if (alias != null) {
            alias = Normalizer.normalize(alias.trim(), Normalizer.Form.NFC);
        }
    }
}
