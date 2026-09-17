package com.rumbou.backend.dto.response;

import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;

// Nunca expone el hash de la contrasena ni los campos de gamificacion.
public record UsuarioResponse(
        Long id,
        String email,
        String nombre,
        Role role
) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getEmail(), usuario.getNombre(), usuario.getRole());
    }
}
