package com.rumbou.backend.academico;

import com.rumbou.backend.shared.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Define cuantas preguntas de cada Tema entran en el examen de un Area,
// y bajo que EsquemaCalificacion se califican. El generador de simulacros
// (modulo examen) recorre estas filas para armar la prueba sin saber
// a que universidad pertenecen.
@Entity
@Table(name = "estructuras_examen")
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
