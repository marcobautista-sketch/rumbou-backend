package com.rumbou.backend.event;

// Se publica cuando un usuario nuevo termina de registrarse. Lo escucha el
// modulo de correo (Zoe) para enviar el email de confirmacion de registro,
// via @TransactionalEventListener(phase = AFTER_COMMIT) + @Async.
public record UsuarioRegistradoEvent(
        Long usuarioId,
        String email,
        String nombre
) {
}
