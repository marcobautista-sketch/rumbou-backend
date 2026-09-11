package com.rumbou.backend.auth;

import com.rumbou.backend.shared.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// JUnit puro con Mockito: AuthService no necesita levantar Spring para
// probarse, sus colaboradores (repos, encoder, publisher) se simulan.
class AuthServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private PasswordEncoder passwordEncoder;
    private ApplicationEventPublisher eventPublisher;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        authService = new AuthService(
                usuarioRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                mock(JwtService.class),
                mock(AuthenticationManager.class),
                eventPublisher
        );
    }

    @Test
    void forgotPasswordGeneraUnTokenYPublicaElEventoSiElUsuarioExiste() {
        Usuario usuario = new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);
        when(usuarioRepository.findByEmail("postulante@rumbou.com")).thenReturn(Optional.of(usuario));

        authService.forgotPassword("postulante@rumbou.com");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(eventPublisher).publishEvent(any(PasswordResetRequestedEvent.class));
    }

    @Test
    void forgotPasswordNoHaceNadaSiElEmailNoExiste() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        authService.forgotPassword("nadie@rumbou.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void resetPasswordActualizaLaContrasenaYMarcaElTokenComoUsado() {
        Usuario usuario = new Usuario("postulante@rumbou.com", "hash-viejo", "Ana", Role.USER);
        PasswordResetToken token = new PasswordResetToken(usuario, "token-valido", LocalDateTime.now().plusMinutes(10));
        when(passwordResetTokenRepository.findByToken("token-valido")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("nueva-contrasena")).thenReturn("hash-nuevo");

        authService.resetPassword("token-valido", "nueva-contrasena");

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
        assertThat(token.isUsado()).isTrue();
        verify(usuarioRepository).save(usuario);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void resetPasswordFallaSiElTokenNoExiste() {
        when(passwordResetTokenRepository.findByToken("token-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("token-inexistente", "nueva-contrasena"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPasswordFallaSiElTokenYaExpiro() {
        Usuario usuario = new Usuario("postulante@rumbou.com", "hash-viejo", "Ana", Role.USER);
        PasswordResetToken tokenExpirado = new PasswordResetToken(usuario, "token-viejo", LocalDateTime.now().minusMinutes(1));
        when(passwordResetTokenRepository.findByToken("token-viejo")).thenReturn(Optional.of(tokenExpirado));

        assertThatThrownBy(() -> authService.resetPassword("token-viejo", "nueva-contrasena"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPasswordFallaSiElTokenYaFueUsado() {
        Usuario usuario = new Usuario("postulante@rumbou.com", "hash-viejo", "Ana", Role.USER);
        PasswordResetToken tokenUsado = new PasswordResetToken(usuario, "token-usado", LocalDateTime.now().plusMinutes(10));
        tokenUsado.marcarComoUsado();
        when(passwordResetTokenRepository.findByToken("token-usado")).thenReturn(Optional.of(tokenUsado));

        assertThatThrownBy(() -> authService.resetPassword("token-usado", "nueva-contrasena"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
