package com.rumbou.backend.dto.request;

import jakarta.validation.constraints.NotNull;

// La oferta academica ya trae universidad + carrera + area + proceso, y con ella
// el puntaje del ultimo ingresante contra el que se calcula el IP.
public record CrearObjetivoRequest(
        @NotNull Long ofertaAcademicaId
) {
}
