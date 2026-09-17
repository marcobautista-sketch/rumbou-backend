package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "universidades")
public class Universidad extends BaseEntity {

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String siglas;

    @Column(nullable = false)
    private int puntajeMaximo;

    @Column(nullable = false)
    private int totalPreguntas;

    public Universidad() {
    }

    public Universidad(String nombre, String siglas, int puntajeMaximo, int totalPreguntas) {
        this.nombre = nombre;
        this.siglas = siglas;
        this.puntajeMaximo = puntajeMaximo;
        this.totalPreguntas = totalPreguntas;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSiglas() {
        return siglas;
    }

    public void setSiglas(String siglas) {
        this.siglas = siglas;
    }

    public int getPuntajeMaximo() {
        return puntajeMaximo;
    }

    public void setPuntajeMaximo(int puntajeMaximo) {
        this.puntajeMaximo = puntajeMaximo;
    }

    public int getTotalPreguntas() {
        return totalPreguntas;
    }

    public void setTotalPreguntas(int totalPreguntas) {
        this.totalPreguntas = totalPreguntas;
    }
}
