package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.UsuarioResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario con email " + email));
        return UsuarioResponse.de(usuario);
    }

    // Un admin no puede quitarse su propio rol: si fuera el unico, el sistema
    // se quedaria sin administradores y nadie podria volver a asignar uno.
    @Transactional
    public UsuarioResponse cambiarRol(Long id, Role nuevoRol, Usuario solicitante) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario con id " + id));

        if (usuario.getId().equals(solicitante.getId()) && nuevoRol != Role.ADMIN) {
            throw new InvalidOperationException("No puedes quitarte tu propio rol de administrador");
        }

        usuario.setRole(nuevoRol);
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    // Si la cuenta existe solo se promueve (su contrasena se respeta); si no,
    // se crea. Lo usa AdminBootstrapRunner al arrancar.
    @Transactional
    public void asegurarAdmin(String email, String password, String nombre) {
        Usuario existente = usuarioRepository.findByEmail(email).orElse(null);

        if (existente == null) {
            usuarioRepository.save(new Usuario(email, passwordEncoder.encode(password), nombre, Role.ADMIN));
            log.info("Usuario administrador {} creado", email);
            return;
        }

        if (existente.getRole() == Role.ADMIN) {
            return;
        }

        existente.setRole(Role.ADMIN);
        usuarioRepository.save(existente);
        log.info("Usuario {} promovido a administrador", email);
    }
}
