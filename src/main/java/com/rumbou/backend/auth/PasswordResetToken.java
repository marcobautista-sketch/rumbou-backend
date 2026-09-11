package com.rumbou.backend.auth;

import com.rumbou.backend.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private boolean usado;

    public PasswordResetToken() {
    }

    public PasswordResetToken(Usuario usuario, String token, LocalDateTime fechaExpiracion) {
        this.usuario = usuario;
        this.token = token;
        this.fechaExpiracion = fechaExpiracion;
        this.usado = false;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public boolean isUsado() {
        return usado;
    }

    public void marcarComoUsado() {
        this.usado = true;
    }

    public boolean estaVigente() {
        return !usado && fechaExpiracion.isAfter(LocalDateTime.now());
    }
}
