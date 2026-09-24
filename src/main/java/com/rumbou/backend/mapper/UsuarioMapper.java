package com.rumbou.backend.mapper;

import com.rumbou.backend.dto.response.GamificacionResponse;
import com.rumbou.backend.dto.response.LogroResponse;
import com.rumbou.backend.dto.response.UsuarioResponse;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.entity.UsuarioLogro;

import java.util.List;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getEmail(), usuario.getNombre(), usuario.getRole());
    }

    public static GamificacionResponse toGamificacionResponse(Usuario usuario, List<UsuarioLogro> logros) {
        return new GamificacionResponse(
                usuario.getXpTotal(),
                usuario.getXpSemanal(),
                usuario.getCurrentStreak(),
                usuario.getLongestStreak(),
                usuario.getLastActivityDate(),
                logros.stream().map(UsuarioMapper::toLogroResponse).toList());
    }

    private static LogroResponse toLogroResponse(UsuarioLogro usuarioLogro) {
        return new LogroResponse(
                usuarioLogro.getLogro().getId(),
                usuarioLogro.getLogro().getNombre(),
                usuarioLogro.getLogro().getDescripcion(),
                usuarioLogro.getFechaDesbloqueo());
    }
}
