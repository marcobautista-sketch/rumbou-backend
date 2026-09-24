package com.rumbou.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "suscripciones", indexes = @Index(name = "idx_suscripcion_usuario_estado", columnList = "usuario_id, estado"))
public class Suscripcion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSuscripcion estado;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private String mercadoPagoPreapprovalId;
    private String externalReference;

    public Suscripcion() { }

    public Suscripcion(Usuario usuario, Plan plan, EstadoSuscripcion estado) {
        this.usuario = usuario;
        this.plan = plan;
        this.estado = estado;
    }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }

    public EstadoSuscripcion getEstado() { return estado; }
    public void setEstado(EstadoSuscripcion estado) { this.estado = estado; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getMercadoPagoPreapprovalId() { return mercadoPagoPreapprovalId; }
    public void setMercadoPagoPreapprovalId(String mercadoPagoPreapprovalId) { this.mercadoPagoPreapprovalId = mercadoPagoPreapprovalId; }

    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }

}