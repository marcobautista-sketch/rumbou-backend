package com.rumbou.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// La web guarda el refresh token en esta cookie: httpOnly (JavaScript no la lee) y
// limitada a /api/v1/auth, asi solo viaja a refresh y logout. El movil usa el cuerpo.
@Component
public class RefreshTokenCookie {

    public static final String NOMBRE = "rumbou_refresh";
    private static final String RUTA = "/api/v1/auth";

    private final Duration duracion;
    private final boolean segura;

    public RefreshTokenCookie(@Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs,
                              @Value("${app.auth.refresh-cookie-secure}") boolean segura) {
        this.duracion = Duration.ofMillis(refreshTokenExpirationMs);
        this.segura = segura;
    }

    public String crear(String refreshToken) {
        return construir(refreshToken, duracion);
    }

    public String borrar() {
        return construir("", Duration.ZERO);
    }

    private String construir(String valor, Duration maxAge) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Strict")
                .path(RUTA)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
