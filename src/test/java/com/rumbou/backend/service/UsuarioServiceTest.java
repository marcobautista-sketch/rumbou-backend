package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.UsuarioResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.UsuarioRepository;
import com.rumbou.backend.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private UsuarioService usuarioService;
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        currentUserService = mock(CurrentUserService.class);
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder, currentUserService);
        when(currentUserService.getUsuarioId()).thenReturn(1L);

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario usuario(Long id, String email, Role role) {
        Usuario usuario = new Usuario(email, "hash", "Ana", role);
        usuario.setId(id);
        return usuario;
    }

    @Test
    void cambiarRolPromueveAUnUsuarioAAdmin() {
        Usuario admin = usuario(1L, "admin@rumbou.com", Role.ADMIN);
        Usuario postulante = usuario(2L, "ana@rumbou.com", Role.USER);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(postulante));

        UsuarioResponse respuesta = usuarioService.cambiarRol(2L, Role.ADMIN);

        assertThat(respuesta.role()).isEqualTo(Role.ADMIN);
        assertThat(postulante.getRole()).isEqualTo(Role.ADMIN);
        verify(usuarioRepository).save(postulante);
    }

    @Test
    void cambiarRolLanza404SiElUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.cambiarRol(99L, Role.ADMIN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unAdminNoPuedeQuitarseSuPropioRol() {
        Usuario admin = usuario(1L, "admin@rumbou.com", Role.ADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.cambiarRol(1L, Role.USER))
                .isInstanceOf(InvalidOperationException.class);

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void unAdminSiPuedeDegradarAOtroAdmin() {
        Usuario admin = usuario(1L, "admin@rumbou.com", Role.ADMIN);
        Usuario otroAdmin = usuario(2L, "otro@rumbou.com", Role.ADMIN);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(otroAdmin));

        UsuarioResponse respuesta = usuarioService.cambiarRol(2L, Role.USER);

        assertThat(respuesta.role()).isEqualTo(Role.USER);
    }

    @Test
    void asegurarAdminCreaLaCuentaConLaContrasenaHasheadaSiNoExiste() {
        when(usuarioRepository.findByEmail("admin@rumbou.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secreta")).thenReturn("hash-secreta");

        usuarioService.asegurarAdmin("admin@rumbou.com", "secreta", "Administrador");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario creado = captor.getValue();
        assertThat(creado.getEmail()).isEqualTo("admin@rumbou.com");
        assertThat(creado.getPasswordHash()).isEqualTo("hash-secreta");
        assertThat(creado.getNombre()).isEqualTo("Administrador");
        assertThat(creado.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void asegurarAdminPromueveUnaCuentaExistenteSinTocarSuContrasena() {
        Usuario existente = usuario(5L, "marco@rumbou.com", Role.USER);
        when(usuarioRepository.findByEmail("marco@rumbou.com")).thenReturn(Optional.of(existente));

        usuarioService.asegurarAdmin("marco@rumbou.com", "otra-clave", "Administrador");

        assertThat(existente.getRole()).isEqualTo(Role.ADMIN);
        assertThat(existente.getPasswordHash()).isEqualTo("hash");
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository).save(existente);
    }

    @Test
    void asegurarAdminNoHaceNadaSiYaEsAdmin() {
        Usuario existente = usuario(5L, "marco@rumbou.com", Role.ADMIN);
        when(usuarioRepository.findByEmail("marco@rumbou.com")).thenReturn(Optional.of(existente));

        usuarioService.asegurarAdmin("marco@rumbou.com", "otra-clave", "Administrador");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void buscarPorEmailLanza404SiNoExiste() {
        when(usuarioRepository.findByEmail("nadie@rumbou.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorEmail("nadie@rumbou.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
