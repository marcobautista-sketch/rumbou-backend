package com.rumbou.backend.mapper;

import com.rumbou.backend.dto.response.OfertaAcademicaResponse;
import com.rumbou.backend.entity.OfertaAcademica;

public final class OfertaAcademicaMapper {

    private OfertaAcademicaMapper() {
    }

    public static OfertaAcademicaResponse toResponse(OfertaAcademica oferta) {
        return new OfertaAcademicaResponse(
                oferta.getId(),
                oferta.getUniversidad().getSiglas(),
                oferta.getArea().getId(),
                oferta.getArea().getCodigo(),
                oferta.getArea().getNombre(),
                oferta.getCarrera().getNombre(),
                oferta.getCarrera().getFacultad(),
                oferta.getProcesoAdmision(),
                oferta.getPuntajeUltimoIngresante(),
                oferta.getVacantes());
    }
}
