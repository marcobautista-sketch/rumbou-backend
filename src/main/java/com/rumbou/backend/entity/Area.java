package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "areas")
public class Area extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "universidad_id", nullable = false)
    private Universidad universidad;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 20)
    private String procesoProximo;

    private LocalDate fechaProximoExamen;

    // false mientras la universidad no publica su cronograma: la fecha es una estimacion.
    private Boolean fechaExamenConfirmada;

    public Area() {
    }

    public Area(Universidad universidad, String codigo, String nombre) {
        this.universidad = universidad;
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public Universidad getUniversidad() {
        return universidad;
    }

    public void setUniversidad(Universidad universidad) {
        this.universidad = universidad;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getProcesoProximo() {
        return procesoProximo;
    }

    public void setProcesoProximo(String procesoProximo) {
        this.procesoProximo = procesoProximo;
    }

    public LocalDate getFechaProximoExamen() {
        return fechaProximoExamen;
    }

    public void setFechaProximoExamen(LocalDate fechaProximoExamen) {
        this.fechaProximoExamen = fechaProximoExamen;
    }

    public Boolean getFechaExamenConfirmada() {
        return fechaExamenConfirmada;
    }

    public void setFechaExamenConfirmada(Boolean fechaExamenConfirmada) {
        this.fechaExamenConfirmada = fechaExamenConfirmada;
    }
}
