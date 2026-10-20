package com.rumbou.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(
        @NotBlank @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String nombre,
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres") String apellidos,
        @Pattern(regexp = CelularPolicy.REGEX, message = CelularPolicy.MENSAJE) String telefono
) {
}
