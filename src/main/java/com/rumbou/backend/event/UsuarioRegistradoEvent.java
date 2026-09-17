package com.rumbou.backend.event;

// Lo publica AuthService al registrarse; lo escucha RegistroConfirmacionListener.
public record UsuarioRegistradoEvent(
        Long usuarioId,
        String email,
        String nombre
) {
}
