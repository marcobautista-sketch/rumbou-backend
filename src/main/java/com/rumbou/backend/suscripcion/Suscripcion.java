package com.rumbou.backend.suscripcion;

import com.rumbou.backend.auth.Usuario;
import com.rumbou.backend.shared.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

// El "contrato" del plan: que usuario lo tiene, que plan, y hasta cuando.
@Entity
@Table(name = "suscripciones")
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

    // getters y setters para todos los campos, igual que en Tema
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