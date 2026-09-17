package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

// Un Logro es una "medalla" definida una sola vez en el catalogo.
// El valor exacto para desbloquearlo vive en valorRequerido (ej. RACHA_DIAS = 7).
@Entity
@Table(name = "logros")
public class Logro extends BaseEntity {

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoLogro condicion;

    @Column(nullable = false)
    private int valorRequerido;

    public Logro() {
    }

    public Logro(String nombre, String descripcion, TipoLogro condicion, int valorRequerido) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.condicion = condicion;
        this.valorRequerido = valorRequerido;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoLogro getCondicion() {
        return condicion;
    }

    public void setCondicion(TipoLogro condicion) {
        this.condicion = condicion;
    }

    public int getValorRequerido() {
        return valorRequerido;
    }

    public void setValorRequerido(int valorRequerido) {
        this.valorRequerido = valorRequerido;
    }
}