package com.rumbou.backend.contenido;

import com.rumbou.backend.academico.Tema;
import com.rumbou.backend.shared.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.List;

// Una pregunta pertenece a un Tema, nunca a una universidad. Asi el mismo
// banco de preguntas sirve para UNI y UNMSM (ver seccion 3.4 del documento
// de decisiones). Estructura completa; la generacion con IA y la validacion
// de calidad las construye Persona A.
@Entity
@Table(name = "preguntas")
public class Pregunta extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tema_id", nullable = false)
    private Tema tema;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @ElementCollection
    @CollectionTable(name = "pregunta_alternativas", joinColumns = @JoinColumn(name = "pregunta_id"))
    @OrderColumn(name = "orden")
    @Column(name = "texto", nullable = false)
    private List<String> alternativas;

    @Column(nullable = false)
    private int claveCorrecta;

    @Column(columnDefinition = "TEXT")
    private String explicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Dificultad dificultad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigenPregunta origen;

    @Column(nullable = false)
    private boolean aprobada;

    public Pregunta() {
    }

    public Pregunta(Tema tema, String enunciado, List<String> alternativas, int claveCorrecta,
                     String explicacion, Dificultad dificultad, OrigenPregunta origen, boolean aprobada) {
        this.tema = tema;
        this.enunciado = enunciado;
        this.alternativas = alternativas;
        this.claveCorrecta = claveCorrecta;
        this.explicacion = explicacion;
        this.dificultad = dificultad;
        this.origen = origen;
        this.aprobada = aprobada;
    }

    public Tema getTema() {
        return tema;
    }

    public void setTema(Tema tema) {
        this.tema = tema;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public List<String> getAlternativas() {
        return alternativas;
    }

    public void setAlternativas(List<String> alternativas) {
        this.alternativas = alternativas;
    }

    public int getClaveCorrecta() {
        return claveCorrecta;
    }

    public void setClaveCorrecta(int claveCorrecta) {
        this.claveCorrecta = claveCorrecta;
    }

    public String getExplicacion() {
        return explicacion;
    }

    public void setExplicacion(String explicacion) {
        this.explicacion = explicacion;
    }

    public Dificultad getDificultad() {
        return dificultad;
    }

    public void setDificultad(Dificultad dificultad) {
        this.dificultad = dificultad;
    }

    public OrigenPregunta getOrigen() {
        return origen;
    }

    public void setOrigen(OrigenPregunta origen) {
        this.origen = origen;
    }

    public boolean isAprobada() {
        return aprobada;
    }

    public void setAprobada(boolean aprobada) {
        this.aprobada = aprobada;
    }
}
