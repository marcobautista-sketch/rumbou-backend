package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "temas")
public class Tema extends BaseEntity {

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AreaConocimiento areaConocimiento;

    // Temario oficial del prospecto; lo usa el generador de preguntas para acotar
    // lo que genera. TEXT y no @Lob: en PostgreSQL @Lob sobre String mapea a oid.
    @Column(columnDefinition = "TEXT")
    private String temario;

    public Tema() {
    }

    public Tema(String nombre, AreaConocimiento areaConocimiento) {
        this.nombre = nombre;
        this.areaConocimiento = areaConocimiento;
    }

    public Tema(String nombre, AreaConocimiento areaConocimiento, String temario) {
        this(nombre, areaConocimiento);
        this.temario = temario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public AreaConocimiento getAreaConocimiento() {
        return areaConocimiento;
    }

    public void setAreaConocimiento(AreaConocimiento areaConocimiento) {
        this.areaConocimiento = areaConocimiento;
    }

    public String getTemario() {
        return temario;
    }

    public void setTemario(String temario) {
        this.temario = temario;
    }
}
