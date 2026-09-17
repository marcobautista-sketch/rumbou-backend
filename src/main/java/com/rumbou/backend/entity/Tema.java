package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

// Una Pregunta (modulo contenido) pertenece a un Tema, nunca a una universidad.
// Eso es lo que permite reutilizar el mismo banco de preguntas entre UNI y UNMSM.
@Entity
@Table(name = "temas")
public class Tema extends BaseEntity {

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AreaConocimiento areaConocimiento;

    // Lista oficial de subtemas del prospecto de admision. La usa el generador
    // de preguntas con Gemini (modulo contenido) para acotar el nivel y el
    // contenido de lo que genera. Es opcional a proposito: el campo existe
    // desde ya para desbloquear ese generador, y el texto se va completando
    // despues sin volver a tocar codigo.
    //
    // TEXT y no @Lob: en PostgreSQL, @Lob sobre un String mapea al tipo oid
    // (pensado para binarios) y falla al insertar texto normal.
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
