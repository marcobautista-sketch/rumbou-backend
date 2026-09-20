package com.rumbou.backend.dto.response;

// Semaforo del indice de preparacion (IP = PSP / puntaje del ultimo ingresante).
// No se guarda en la base: se deduce del IP cada vez que se arma la respuesta.
public enum EstadoPreparacion {
    HOLGADO(1.10, "Zona de ingreso holgada"),
    AJUSTADO(1.00, "Zona de ingreso ajustada"),
    CERCA(0.85, "Cerca, falta poco"),
    REFORZAR(Double.NEGATIVE_INFINITY, "Necesitas reforzar");

    private final double ipMinimo;
    private final String descripcion;

    EstadoPreparacion(double ipMinimo, String descripcion) {
        this.ipMinimo = ipMinimo;
        this.descripcion = descripcion;
    }

    // Los valores estan ordenados de mayor a menor: gana el primero cuyo minimo se alcanza.
    public static EstadoPreparacion desde(double ip) {
        for (EstadoPreparacion estado : values()) {
            if (ip >= estado.ipMinimo) {
                return estado;
            }
        }
        return REFORZAR;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
