package com.rumbou.backend.gamificacion;

import com.rumbou.backend.auth.Usuario;
import com.rumbou.backend.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

// M:N con atributos entre Usuario y Logro: cada fila dice cuando un usuario
// desbloqueo un logro. La constraint unica evita que se gane el mismo logro 2 veces.
@Entity
@Table(name = "usuarios_logros",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "logro_id"}))
public class
UsuarioLogro extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logro_id", nullable = false)
    private Logro logro;

    @Column(nullable = false)
    private LocalDateTime fechaDesbloqueo;

    public UsuarioLogro() {
    }

    public UsuarioLogro(Usuario usuario, Logro logro, LocalDateTime fechaDesbloqueo) {
        this.usuario = usuario;
        this.logro = logro;
        this.fechaDesbloqueo = fechaDesbloqueo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Logro getLogro() {
        return logro;
    }

    public void setLogro(Logro logro) {
        this.logro = logro;
    }

    public LocalDateTime getFechaDesbloqueo() {
        return fechaDesbloqueo;
    }

    public void setFechaDesbloqueo(LocalDateTime fechaDesbloqueo) {
        this.fechaDesbloqueo = fechaDesbloqueo;
    }
}