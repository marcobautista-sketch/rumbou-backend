package com.rumbou.backend.auth;

import com.rumbou.backend.auth.dto.AuthResponse;
import com.rumbou.backend.auth.dto.LoginRequest;
import com.rumbou.backend.auth.dto.RegisterRequest;
import com.rumbou.backend.shared.exception.DuplicateResourceException;
import com.rumbou.backend.shared.exception.InvalidTokenException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = new Usuario(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nombre(),
                Role.USER
        );
        usuarioRepository.save(usuario);

        return new AuthResponse(
                jwtService.generateAccessToken(usuario),
                jwtService.generateRefreshToken(usuario)
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado pero no encontrado"));

        return new AuthResponse(
                jwtService.generateAccessToken(usuario),
                jwtService.generateRefreshToken(usuario)
        );
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("El token proporcionado no es un refresh token valido");
        }

        String email = jwtService.extractEmail(refreshToken);

        if (!jwtService.isTokenValid(refreshToken, email)) {
            throw new InvalidTokenException("El refresh token es invalido o expiro");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("El usuario del token ya no existe"));

        return new AuthResponse(
                jwtService.generateAccessToken(usuario),
                jwtService.generateRefreshToken(usuario)
        );
    }
}
