package com.rumbou.backend.academico;

import com.rumbou.backend.shared.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Define cuantas preguntas de cada Tema entran en el examen de un Area,
// y bajo que EsquemaCalificacion se califican. El generador de simulacros
// (modulo examen) recorre estas filas para armar la prueba sin saber
// a que universidad pertenecen.
//
// La restriccion unica (area_id, tema_id) es necesaria porque el esquema de
// calificacion se resuelve POR TEMA al finalizar un simulacro: si el mismo
// Tema apareciera en dos bloques de la misma Area, no habria forma de saber
// con que esquema calificar sus preguntas. Un tema que se evalua en dos
// bloques distintos debe modelarse como dos Tema distintos (por ejemplo
// "Habilidad Logico-Matematica" en Habilidades y "Aritmetica" en Conocimientos).
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
