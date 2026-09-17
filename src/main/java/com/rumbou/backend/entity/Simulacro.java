package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "simulacros")
public class Simulacro extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSimulacro tipo;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    private Double puntajeObtenido;

    // Puntaje Simulado Proyectado: puntajeObtenido escalado al maximo de la universidad.
    private Double psp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSimulacro estado;

    public Simulacro() {
    }

    public Simulacro(Usuario usuario, Area area, TipoSimulacro tipo, LocalDateTime fechaInicio) {
        this.usuario = usuario;
        this.area = area;
        this.tipo = tipo;
        this.fechaInicio = fechaInicio;
        this.estado = EstadoSimulacro.EN_CURSO;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public TipoSimulacro getTipo() {
        return tipo;
    }

    public void setTipo(TipoSimulacro tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Double getPuntajeObtenido() {
        return puntajeObtenido;
    }

    public void setPuntajeObtenido(Double puntajeObtenido) {
        this.puntajeObtenido = puntajeObtenido;
    }

    public Double getPsp() {
        return psp;
    }

    public void setPsp(Double psp) {
        this.psp = psp;
    }

    public EstadoSimulacro getEstado() {
        return estado;
    }

    public void setEstado(EstadoSimulacro estado) {
        this.estado = estado;
    }
}
