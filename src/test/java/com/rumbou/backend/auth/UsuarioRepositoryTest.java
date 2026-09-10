package com.rumbou.backend.auth;

import com.rumbou.backend.shared.AbstractContainerBaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void guardaYRecuperaUnUsuarioPorEmail() {
        Usuario usuario = new Usuario("postulante@rumbou.com", "hash-de-prueba", "Ana", Role.USER);

        usuarioRepository.save(usuario);

        Optional<Usuario> encontrado = usuarioRepository.findByEmail("postulante@rumbou.com");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNombre()).isEqualTo("Ana");
        assertThat(encontrado.get().getRole()).isEqualTo(Role.USER);
    }
}
