package com.rumbou.backend.event;

// Se publica cuando un usuario pide resetear su contrasena. Lo escucha el
// modulo de correo (Zoe) para enviar el email con el link/token, via
// @TransactionalEventListener(phase = AFTER_COMMIT) + @Async.
public record PasswordResetRequestedEvent(
        Long usuarioId,
        String email,
        String nombre,
        String token
) {
}
