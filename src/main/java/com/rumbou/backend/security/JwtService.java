package com.rumbou.backend.security;

import com.rumbou.backend.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
                       @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(Usuario usuario) {
        return buildToken(usuario, accessTokenExpirationMs, Map.of(
                "userId", usuario.getId(),
                "role", usuario.getRole().name()
        ));
    }

    public String generateRefreshToken(Usuario usuario) {
        return buildToken(usuario, refreshTokenExpirationMs, Map.of(
                "userId", usuario.getId(),
                CLAIM_TYPE, TYPE_REFRESH
        ));
    }

    private String buildToken(Usuario usuario, long expirationMs, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claims(extraClaims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(extractAllClaims(token).get(CLAIM_TYPE, String.class));
    }

    public boolean isTokenValid(String token, String expectedEmail) {
        String email = extractEmail(token);
        return email.equals(expectedEmail) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
