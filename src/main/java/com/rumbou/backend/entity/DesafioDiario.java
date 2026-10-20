package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Una pregunta por usuario y por dia: la restriccion unica impide un segundo desafio el mismo dia.
@Entity
@Table(name = "desafios_diarios",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "fecha"}))
public class DesafioDiario extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    private Integer alternativaMarcada;
    private Boolean esCorrecta;
    private Integer xpGanado;
    private LocalDateTime fechaRespuesta;

    public DesafioDiario() {
    }

    public DesafioDiario(Usuario usuario, LocalDate fecha, Pregunta pregunta) {
        this.usuario = usuario;
        this.fecha = fecha;
        this.pregunta = pregunta;
    }

    public boolean estaRespondido() {
        return fechaRespuesta != null;
    }

    public void registrarRespuesta(int alternativaMarcada, boolean esCorrecta, int xpGanado) {
        this.alternativaMarcada = alternativaMarcada;
        this.esCorrecta = esCorrecta;
        this.xpGanado = xpGanado;
        this.fechaRespuesta = LocalDateTime.now();
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Pregunta getPregunta() {
        return pregunta;
    }

    public Integer getAlternativaMarcada() {
        return alternativaMarcada;
    }

    public Boolean getEsCorrecta() {
        return esCorrecta;
    }

    public Integer getXpGanado() {
        return xpGanado;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }
}
