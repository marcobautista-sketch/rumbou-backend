package com.rumbou.backend.security;

import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-bytes-1234567890";

    private JwtService jwtService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRETO, 900_000, 604_800_000);
        usuario = new Usuario("postulante@rumbou.com", "hash", "Ana", Role.ADMIN);
        usuario.setId(7L);
    }

    @Test
    void elAccessTokenLlevaEmailUserIdYRol() {
        String token = jwtService.generateAccessToken(usuario);

        assertThat(jwtService.extractEmail(token)).isEqualTo("postulante@rumbou.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(7L);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtService.isAccessTokenValid(token, "postulante@rumbou.com")).isTrue();
    }

    // Un refresh token robado no debe servir como Bearer para llamar a la API.
    @Test
    void unRefreshTokenNoEsValidoComoAccessToken() {
        String refresh = jwtService.generateRefreshToken(usuario);

        assertThat(jwtService.isRefreshToken(refresh)).isTrue();
        assertThat(jwtService.isAccessTokenValid(refresh, "postulante@rumbou.com")).isFalse();
    }

    @Test
    void unTokenDeOtroUsuarioNoEsValido() {
        String token = jwtService.generateAccessToken(usuario);

        assertThat(jwtService.isAccessTokenValid(token, "otro@rumbou.com")).isFalse();
    }
}
