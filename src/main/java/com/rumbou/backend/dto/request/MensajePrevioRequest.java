package com.rumbou.backend.dto.request;

import com.rumbou.backend.entity.RolMensaje;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MensajePrevioRequest(
        @NotNull RolMensaje rol,
        @NotBlank @Size(max = 1000) String texto
) {
}
