package com.rumbou.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

// Una fila por usuario y por dia: aqui viven los contadores del plan gratuito.
@Entity
@Table(name = "uso_diario",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "fecha"}))
public class UsoDiario extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDate fecha;

    private int simulacrosTema;
    private int simulacrosCompletos;
    private int consultasTutorIA;

    @Version
    private Long version;

    public UsoDiario() { }

    public UsoDiario(Usuario usuario, LocalDate fecha) {
        this.usuario = usuario;
        this.fecha = fecha;
    }

    // getters y setters para usuario, fecha y los 3 contadores (version no necesita setter)
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public int getSimulacrosTema() { return simulacrosTema; }
    public void setSimulacrosTema(int simulacrosTema) { this.simulacrosTema = simulacrosTema; }
    public void incrementarSimulacrosTema() { this.simulacrosTema++; }

    public int getSimulacrosCompletos() { return simulacrosCompletos; }
    public void setSimulacrosCompletos(int simulacrosCompletos) { this.simulacrosCompletos = simulacrosCompletos; }
    public void incrementarSimulacrosCompletos() { this.simulacrosCompletos++; }

    public int getConsultasTutorIA() { return consultasTutorIA; }
    public void setConsultasTutorIA(int consultasTutorIA) { this.consultasTutorIA = consultasTutorIA; }
    public void incrementarConsultasTutorIA() { this.consultasTutorIA++; }

}