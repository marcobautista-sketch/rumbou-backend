package com.rumbou.backend.security;

import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.InvalidTokenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Los servicios protegidos preguntan aqui quien hace el request: lo lee del
// SecurityContext que JwtAuthenticationFilter llena al validar el token.
@Component
public class CurrentUserService {

    public Usuario getUsuario() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Usuario usuario)) {
            throw new InvalidTokenException("Se requiere un usuario autenticado");
        }
        return usuario;
    }

    public Long getUsuarioId() {
        return getUsuario().getId();
    }

    public boolean esAdmin() {
        return getUsuario().getRole() == Role.ADMIN;
    }

    // ADMIN y REVIEWER ven las preguntas sin aprobar, porque son quienes las revisan.
    public boolean puedeRevisarPreguntas() {
        Role role = getUsuario().getRole();
        return role == Role.ADMIN || role == Role.REVIEWER;
    }
}
