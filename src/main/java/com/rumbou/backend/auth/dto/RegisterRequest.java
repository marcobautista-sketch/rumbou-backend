package com.rumbou.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres") String password,
        @NotBlank String nombre
) {
}
