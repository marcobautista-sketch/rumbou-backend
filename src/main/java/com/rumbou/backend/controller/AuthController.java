package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.ForgotPasswordRequest;
import com.rumbou.backend.dto.request.LoginRequest;
import com.rumbou.backend.dto.request.RefreshRequest;
import com.rumbou.backend.dto.request.RegisterRequest;
import com.rumbou.backend.dto.request.ResetPasswordRequest;
import com.rumbou.backend.dto.response.AuthResponse;
import com.rumbou.backend.security.RefreshTokenCookie;
import com.rumbou.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookie refreshTokenCookie;

    public AuthController(AuthService authService, RefreshTokenCookie refreshTokenCookie) {
        this.authService = authService;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return conCookie(HttpStatus.CREATED, authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return conCookie(HttpStatus.OK, authService.login(request));
    }

    // El movil manda el refresh token en el cuerpo; la web, en la cookie.
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody(required = false) RefreshRequest request,
            @CookieValue(name = RefreshTokenCookie.NOMBRE, required = false) String refreshTokenDeCookie) {
        String refreshToken = request != null ? request.refreshToken() : refreshTokenDeCookie;
        return conCookie(HttpStatus.OK, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.borrar())
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> conCookie(HttpStatus status, AuthResponse response) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.crear(response.refreshToken()))
                .body(response);
    }
}
