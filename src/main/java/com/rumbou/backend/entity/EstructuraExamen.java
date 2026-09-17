package com.rumbou.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Cuantas preguntas de cada Tema entran en un Area y con que EsquemaCalificacion
// se califican. La restriccion unica (area, tema) existe porque el esquema se
// resuelve por tema al calificar: un tema evaluado en dos bloques se modela
// como dos Tema distintos.
@Entity
@Table(name = "estructuras_examen",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_estructura_area_tema",
                columnNames = {"area_id", "tema_id"}))
public class EstructuraExamen extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "esquema_id", nullable = false)
    private EsquemaCalificacion esquema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tema_id", nullable = false)
    private Tema tema;

    private int cantidadPreguntas;
    private int orden;

    public EstructuraExamen() {
    }

    public EstructuraExamen(Area area, EsquemaCalificacion esquema, Tema tema,
                             int cantidadPreguntas, int orden) {
        this.area = area;
        this.esquema = esquema;
        this.tema = tema;
        this.cantidadPreguntas = cantidadPreguntas;
        this.orden = orden;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public EsquemaCalificacion getEsquema() {
        return esquema;
    }

    public void setEsquema(EsquemaCalificacion esquema) {
        this.esquema = esquema;
    }

    public Tema getTema() {
        return tema;
    }

    public void setTema(Tema tema) {
        this.tema = tema;
    }

    public int getCantidadPreguntas() {
        return cantidadPreguntas;
    }

    public void setCantidadPreguntas(int cantidadPreguntas) {
        this.cantidadPreguntas = cantidadPreguntas;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }
}
