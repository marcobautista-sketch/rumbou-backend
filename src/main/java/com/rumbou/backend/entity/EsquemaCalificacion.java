package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Las reglas de puntaje viven aqui como datos, nunca como constantes en el codigo.
// El CalificadorService (modulo examen) recibe filas de esta tabla como parametro.
@Entity
@Table(name = "esquemas_calificacion")
public class EsquemaCalificacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "universidad_id", nullable = false)
    private Universidad universidad;

    @Column(nullable = false)
    private String nombreBloque;

    @Column(nullable = false)
    private double valorAcierto;

    @Column(nullable = false)
    private double valorPenalidad;

    @Column(nullable = false)
    private double puntajeMaximoBloque;

    private int orden;

    public EsquemaCalificacion() {
    }

    public EsquemaCalificacion(Universidad universidad, String nombreBloque, double valorAcierto,
                                double valorPenalidad, double puntajeMaximoBloque, int orden) {
        this.universidad = universidad;
        this.nombreBloque = nombreBloque;
        this.valorAcierto = valorAcierto;
        this.valorPenalidad = valorPenalidad;
        this.puntajeMaximoBloque = puntajeMaximoBloque;
        this.orden = orden;
    }

    public Universidad getUniversidad() {
        return universidad;
    }

    public void setUniversidad(Universidad universidad) {
        this.universidad = universidad;
    }

    public String getNombreBloque() {
        return nombreBloque;
    }

    public void setNombreBloque(String nombreBloque) {
        this.nombreBloque = nombreBloque;
    }

    public double getValorAcierto() {
        return valorAcierto;
    }

    public void setValorAcierto(double valorAcierto) {
        this.valorAcierto = valorAcierto;
    }

    public double getValorPenalidad() {
        return valorPenalidad;
    }

    public void setValorPenalidad(double valorPenalidad) {
        this.valorPenalidad = valorPenalidad;
    }

    public double getPuntajeMaximoBloque() {
        return puntajeMaximoBloque;
    }

    public void setPuntajeMaximoBloque(double puntajeMaximoBloque) {
        this.puntajeMaximoBloque = puntajeMaximoBloque;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }
}
