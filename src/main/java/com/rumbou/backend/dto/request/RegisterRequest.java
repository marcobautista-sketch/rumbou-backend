package com.rumbou.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// apellidos y telefono son opcionales: la web los pide en el registro detallado,
// pero un cliente que solo envia email, password y nombre sigue funcionando.
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MENSAJE) String password,
        @NotBlank @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String nombre,
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres") String apellidos,
        @Pattern(regexp = CelularPolicy.REGEX, message = CelularPolicy.MENSAJE) String telefono
) {

    public RegisterRequest(String email, String password, String nombre) {
        this(email, password, nombre, null, null);
    }
}
