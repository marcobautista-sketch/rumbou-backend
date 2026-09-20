package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.OfertaAcademicaResponse;
import com.rumbou.backend.entity.OfertaAcademica;
import com.rumbou.backend.repository.OfertaAcademicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Lectura del catalogo academico: sin esto el postulante no tendria como saber
// el id de la oferta a la que quiere apuntar su objetivo.
@Service
public class CatalogoService {

    private final OfertaAcademicaRepository ofertaAcademicaRepository;

    public CatalogoService(OfertaAcademicaRepository ofertaAcademicaRepository) {
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
    }

    @Transactional(readOnly = true)
    public List<OfertaAcademicaResponse> buscarOfertas(String universidad, String area, String carrera) {
        return ofertaAcademicaRepository
                .buscar(enBlancoEsNulo(universidad), enBlancoEsNulo(area), enBlancoEsNulo(carrera)).stream()
                .map(this::aResponse)
                .toList();
    }

    // Un parametro vacio en la URL (?carrera=) vale lo mismo que no enviarlo.
    private String enBlancoEsNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private OfertaAcademicaResponse aResponse(OfertaAcademica oferta) {
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
