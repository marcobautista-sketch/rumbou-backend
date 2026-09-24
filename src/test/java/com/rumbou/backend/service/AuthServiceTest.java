package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.RegisterRequest;
import com.rumbou.backend.entity.PasswordResetToken;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.event.PasswordResetRequestedEvent;
import com.rumbou.backend.event.UsuarioRegistradoEvent;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.exception.InvalidTokenException;
import com.rumbou.backend.repository.PasswordResetTokenRepository;
import com.rumbou.backend.repository.UsuarioRepository;
import com.rumbou.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private PasswordEncoder passwordEncoder;
    private ApplicationEventPublisher eventPublisher;
    private AuthService authService;
    private JwtService jwtService;
    private AuthenticationManager authenticationManager;

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-bytes-1234567890";

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        jwtService = new JwtService(SECRETO, 900_000, 604_800_000);
        authenticationManager = mock(AuthenticationManager.class);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario guardado = invocation.getArgument(0);
            guardado.setId(1L);
            return guardado;
        });

        authService = new AuthService(
                usuarioRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                eventPublisher
        );
    }

    @Test
    void registerPublicaUsuarioRegistradoEventAlCrearLaCuenta() {
        when(usuarioRepository.existsByEmailIgnoreCase("nueva@rumbou.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");

        authService.register(new RegisterRequest("nueva@rumbou.com", "password123", "Ana"));

        verify(eventPublisher).publishEvent(any(UsuarioRegistradoEvent.class));
    }

    @Test
    void registerNoPublicaEventoSiElEmailYaExiste() {
        when(usuarioRepository.existsByEmailIgnoreCase("existente@rumbou.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("existente@rumbou.com", "password123", "Ana")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void forgotPasswordGeneraUnTokenYPublicaElEventoSiElUsuarioExiste() {
        Usuario usuario = new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);
        when(usuarioRepository.findByEmailIgnoreCase("postulante@rumbou.com")).thenReturn(Optional.of(usuario));

        authService.forgotPassword("postulante@rumbou.com");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(eventPublisher).publishEvent(any(PasswordResetRequestedEvent.class));
    }

    @Test
    void forgotPasswordInvalidaLosTokensAnterioresDelUsuario() {
        Usuario usuario = new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);
        usuario.setId(7L);
        PasswordResetToken anterior = new PasswordResetToken(usuario, "token-anterior", LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByEmailIgnoreCase("postulante@rumbou.com")).thenReturn(Optional.of(usuario));
        when(passwordResetTokenRepository.findByUsuarioIdAndUsadoFalse(7L)).thenReturn(List.of(anterior));

        authService.forgotPassword("postulante@rumbou.com");

        assertThat(anterior.isUsado()).isTrue();
        assertThat(anterior.estaVigente()).isFalse();
    }

    @Test
    void forgotPasswordNoHaceNadaSiElEmailNoExiste() {
        when(usuarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

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

    @Test
    void registerGuardaElEmailEnMinusculasYSinEspacios() {
        when(usuarioRepository.existsByEmailIgnoreCase("nueva@rumbou.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hash");

        authService.register(new RegisterRequest("  Nueva@Rumbou.com ", "Password123", "Ana"));

        org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("nueva@rumbou.com");
    }

    @Test
    void loginConCredencialesInvalidasLanzaInvalidCredentialsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("mal"));

        assertThatThrownBy(() -> authService.login(
                new com.rumbou.backend.dto.request.LoginRequest("postulante@rumbou.com", "incorrecta")))
                .isInstanceOf(com.rumbou.backend.exception.InvalidCredentialsException.class);
    }

    @Test
    void refreshConUnTokenMalformadoLanzaInvalidTokenYNoUn500() {
        assertThatThrownBy(() -> authService.refresh("esto-no-es-un-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refreshNoAceptaUnAccessToken() {
        Usuario usuario = new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);
        usuario.setId(7L);
        String accessToken = jwtService.generateAccessToken(usuario);

        assertThatThrownBy(() -> authService.refresh(accessToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refreshConUnRefreshTokenValidoDevuelveTokensNuevos() {
        Usuario usuario = new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);
        usuario.setId(7L);
        when(usuarioRepository.findByEmailIgnoreCase("postulante@rumbou.com")).thenReturn(Optional.of(usuario));

        var respuesta = authService.refresh(jwtService.generateRefreshToken(usuario));

        assertThat(respuesta.accessToken()).isNotBlank();
        assertThat(jwtService.isRefreshToken(respuesta.accessToken())).isFalse();
    }
}
