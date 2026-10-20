package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.ActualizarPerfilRequest;
import com.rumbou.backend.dto.request.CambiarContrasenaRequest;
import com.rumbou.backend.dto.response.UsuarioResponse;
import com.rumbou.backend.entity.Role;

public interface UsuarioService {

    UsuarioResponse obtenerPerfil();

    UsuarioResponse actualizarPerfil(ActualizarPerfilRequest request);

    void cambiarContrasena(CambiarContrasenaRequest request);

    UsuarioResponse buscarPorEmail(String email);

    UsuarioResponse cambiarRol(Long id, Role nuevoRol);

    void asegurarAdmin(String email, String password, String nombre);
}
