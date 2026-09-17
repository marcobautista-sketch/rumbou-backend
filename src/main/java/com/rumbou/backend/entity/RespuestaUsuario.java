package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// M:N con atributos entre Simulacro y Pregunta; puntajeAportado puede ser negativo (penalidad).
@Entity
@Table(name = "respuestas_usuario")
public class RespuestaUsuario extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulacro_id", nullable = false)
    private Simulacro simulacro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    // null si el postulante dejo la pregunta en blanco.
    private Integer alternativaMarcada;

    private Boolean esCorrecta;

    // Positivo, negativo o cero. Null hasta que se califica el simulacro.
    private Double puntajeAportado;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public RespuestaUsuario() {
    }

    public RespuestaUsuario(Simulacro simulacro, Pregunta pregunta, Integer alternativaMarcada,
                             LocalDateTime timestamp) {
        this.simulacro = simulacro;
        this.pregunta = pregunta;
        this.alternativaMarcada = alternativaMarcada;
        this.timestamp = timestamp;
    }

    public Simulacro getSimulacro() {
        return simulacro;
    }

    public void setSimulacro(Simulacro simulacro) {
        this.simulacro = simulacro;
    }

    public Pregunta getPregunta() {
        return pregunta;
    }

    public void setPregunta(Pregunta pregunta) {
        this.pregunta = pregunta;
    }

    public Integer getAlternativaMarcada() {
        return alternativaMarcada;
    }

    public void setAlternativaMarcada(Integer alternativaMarcada) {
        this.alternativaMarcada = alternativaMarcada;
    }

    public Boolean getEsCorrecta() {
        return esCorrecta;
    }

    public void setEsCorrecta(Boolean esCorrecta) {
        this.esCorrecta = esCorrecta;
    }

    public Double getPuntajeAportado() {
        return puntajeAportado;
    }

    public void setPuntajeAportado(Double puntajeAportado) {
        this.puntajeAportado = puntajeAportado;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
