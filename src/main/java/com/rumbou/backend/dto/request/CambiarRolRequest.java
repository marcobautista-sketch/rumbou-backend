package com.rumbou.backend.dto.request;

import com.rumbou.backend.entity.Role;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(
        @NotNull(message = "El rol es obligatorio")
        Role role
) {
}
