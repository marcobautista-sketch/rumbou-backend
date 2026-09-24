package com.rumbou.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

// La carrera a la que apunta un usuario: una OfertaAcademica concreta (universidad,
// carrera, area y proceso). Guarda el ultimo PSP e IP calculados al finalizar un
// simulacro de esa area, para que el panel no los recalcule en cada consulta.
// El semaforo no se persiste: se deduce del IP al leer.
// La restriccion unica evita repetir la misma oferta; desactivar un objetivo y
// volver a elegirlo reutiliza la misma fila.
@Entity
@Table(name = "objetivos_usuario",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "oferta_academica_id"}),
        indexes = @Index(name = "idx_objetivo_usuario_activo", columnList = "usuario_id, activo"))
public class ObjetivoUsuario extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferta_academica_id", nullable = false)
    private OfertaAcademica ofertaAcademica;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    // Null hasta que el usuario finaliza un simulacro en el area de la oferta.
    private Double ultimoPsp;

    private Double ultimoIp;

    private LocalDateTime fechaActualizacion;

    public ObjetivoUsuario() {
    }

    public ObjetivoUsuario(Usuario usuario, OfertaAcademica ofertaAcademica, LocalDateTime fechaCreacion) {
        this.usuario = usuario;
        this.ofertaAcademica = ofertaAcademica;
        this.fechaCreacion = fechaCreacion;
        this.activo = true;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public OfertaAcademica getOfertaAcademica() {
        return ofertaAcademica;
    }

    public void setOfertaAcademica(OfertaAcademica ofertaAcademica) {
        this.ofertaAcademica = ofertaAcademica;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Double getUltimoPsp() {
        return ultimoPsp;
    }

    public void setUltimoPsp(Double ultimoPsp) {
        this.ultimoPsp = ultimoPsp;
    }

    public Double getUltimoIp() {
        return ultimoIp;
    }

    public void setUltimoIp(Double ultimoIp) {
        this.ultimoIp = ultimoIp;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
