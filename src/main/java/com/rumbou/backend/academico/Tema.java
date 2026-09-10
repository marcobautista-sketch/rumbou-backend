package com.rumbou.backend.academico;

import com.rumbou.backend.shared.BaseEntity;
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

    public Tema() {
    }

    public Tema(String nombre, AreaConocimiento areaConocimiento) {
        this.nombre = nombre;
        this.areaConocimiento = areaConocimiento;
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
}
