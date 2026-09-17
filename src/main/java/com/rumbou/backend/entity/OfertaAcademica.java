package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Tabla intermedia M:N con atributos: relaciona Universidad + Carrera + Area
// para un proceso de admision especifico, con el puntaje del ultimo ingresante.
@Entity
@Table(name = "ofertas_academicas")
public class OfertaAcademica extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "universidad_id", nullable = false)
    private Universidad universidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrera_id", nullable = false)
    private Carrera carrera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false)
    private String procesoAdmision;

    @Column(nullable = false)
    private double puntajeUltimoIngresante;

    private int vacantes;

    public OfertaAcademica() {
    }

    public OfertaAcademica(Universidad universidad, Carrera carrera, Area area,
                            String procesoAdmision, double puntajeUltimoIngresante, int vacantes) {
        this.universidad = universidad;
        this.carrera = carrera;
        this.area = area;
        this.procesoAdmision = procesoAdmision;
        this.puntajeUltimoIngresante = puntajeUltimoIngresante;
        this.vacantes = vacantes;
    }

    public Universidad getUniversidad() {
        return universidad;
    }

    public void setUniversidad(Universidad universidad) {
        this.universidad = universidad;
    }

    public Carrera getCarrera() {
        return carrera;
    }

    public void setCarrera(Carrera carrera) {
        this.carrera = carrera;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public String getProcesoAdmision() {
        return procesoAdmision;
    }

    public void setProcesoAdmision(String procesoAdmision) {
        this.procesoAdmision = procesoAdmision;
    }

    public double getPuntajeUltimoIngresante() {
        return puntajeUltimoIngresante;
    }

    public void setPuntajeUltimoIngresante(double puntajeUltimoIngresante) {
        this.puntajeUltimoIngresante = puntajeUltimoIngresante;
    }

    public int getVacantes() {
        return vacantes;
    }

    public void setVacantes(int vacantes) {
        this.vacantes = vacantes;
    }
}
