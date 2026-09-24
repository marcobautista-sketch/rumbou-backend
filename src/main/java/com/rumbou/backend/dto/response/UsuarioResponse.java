package com.rumbou.backend.dto.response;

import com.rumbou.backend.entity.Role;

// Nunca expone el hash de la contrasena ni los campos de gamificacion.
public record UsuarioResponse(
        Long id,
        String email,
        String nombre,
        Role role
) {
}
