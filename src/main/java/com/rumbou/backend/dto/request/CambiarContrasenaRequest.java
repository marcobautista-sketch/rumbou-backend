package com.rumbou.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarContrasenaRequest(
        @NotBlank String contrasenaActual,
        @NotBlank @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MENSAJE) String contrasenaNueva
) {
}
