package com.rumbou.backend.event;

// Lo publica AuthService al pedir un reseteo; lo escucha RecuperacionContrasenaCorreoListener.
public record PasswordResetRequestedEvent(
        Long usuarioId,
        String email,
        String nombre,
        String token
) {
}
