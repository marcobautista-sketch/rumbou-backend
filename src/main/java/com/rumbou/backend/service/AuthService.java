package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.LoginRequest;
import com.rumbou.backend.dto.request.RegisterRequest;
import com.rumbou.backend.dto.response.AuthResponse;
import com.rumbou.backend.entity.PasswordResetToken;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.event.PasswordResetRequestedEvent;
import com.rumbou.backend.event.UsuarioRegistradoEvent;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.exception.InvalidCredentialsException;
import com.rumbou.backend.exception.InvalidTokenException;
import com.rumbou.backend.repository.PasswordResetTokenRepository;
import com.rumbou.backend.repository.UsuarioRepository;
import com.rumbou.backend.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final long RESET_TOKEN_EXPIRATION_MINUTES = 30;

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UsuarioRepository usuarioRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        AuthenticationManager authenticationManager,
                        ApplicationEventPublisher eventPublisher) {
        this.usuarioRepository = usuarioRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = new Usuario(
                email,
                passwordEncoder.encode(request.password()),
                request.nombre(),
                Role.USER
        );
        usuarioRepository.save(usuario);

        eventPublisher.publishEvent(new UsuarioRegistradoEvent(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre()
        ));

        return new AuthResponse(
                jwtService.generateAccessToken(usuario),
                jwtService.generateRefreshToken(usuario)
        );
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizarEmail(request.email());
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException("Credenciales invalidas");
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado pero no encontrado"));

        return new AuthResponse(
                jwtService.generateAccessToken(usuario),
                jwtService.generateRefreshToken(usuario)
        );
    }

    public AuthResponse refresh(String refreshToken) {
        String email = extraerEmailDeRefreshToken(refreshToken);

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidTokenException("El usuario del token ya no existe"));

        return new AuthResponse(
                jwtService.generateAccessToken(usuario),
                jwtService.generateRefreshToken(usuario)
        );
    }

    // Responde igual exista o no el email: si no, el endpoint serviria para
    // averiguar que correos estan registrados.
    @Transactional
    public void forgotPassword(String email) {
        usuarioRepository.findByEmailIgnoreCase(normalizarEmail(email)).ifPresent(usuario -> {
            // Un reseteo nuevo invalida los anteriores para no dejar tokens vivos de mas.
            invalidarTokensVigentesDe(usuario);

            String token = UUID.randomUUID().toString();
            LocalDateTime expiracion = LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRATION_MINUTES);

            passwordResetTokenRepository.save(new PasswordResetToken(usuario, token, expiracion));

            eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                    usuario.getId(),
                    usuario.getEmail(),
                    usuario.getNombre(),
                    token
            ));
        });
    }

    // Firma mala, token expirado o malformado: todo es un 401, nunca un 500.
    private String extraerEmailDeRefreshToken(String refreshToken) {
        try {
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new InvalidTokenException("El token proporcionado no es un refresh token valido");
            }
            String email = jwtService.extractEmail(refreshToken);
            if (!jwtService.isTokenValid(refreshToken, email)) {
                throw new InvalidTokenException("El refresh token es invalido o expiro");
            }
            return email;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("El refresh token es invalido o expiro");
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void invalidarTokensVigentesDe(Usuario usuario) {
        List<PasswordResetToken> vigentes = passwordResetTokenRepository.findByUsuarioIdAndUsadoFalse(usuario.getId());
        vigentes.forEach(PasswordResetToken::marcarComoUsado);
        passwordResetTokenRepository.saveAll(vigentes);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("El token de reseteo no es valido"));

        if (!resetToken.estaVigente()) {
            throw new InvalidTokenException("El token de reseteo es invalido o expiro");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);

        resetToken.marcarComoUsado();
        passwordResetTokenRepository.save(resetToken);
    }
}
