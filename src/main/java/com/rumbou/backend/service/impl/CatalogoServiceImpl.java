package com.rumbou.backend.service.impl;

import com.rumbou.backend.dto.response.OfertaAcademicaResponse;
import com.rumbou.backend.mapper.OfertaAcademicaMapper;
import com.rumbou.backend.repository.OfertaAcademicaRepository;
import com.rumbou.backend.service.CatalogoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogoServiceImpl implements CatalogoService {

    private final OfertaAcademicaRepository ofertaAcademicaRepository;

    public CatalogoServiceImpl(OfertaAcademicaRepository ofertaAcademicaRepository) {
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<OfertaAcademicaResponse> buscarOfertas(String universidad, String area, String carrera) {
        return ofertaAcademicaRepository
                .buscar(enBlancoEsNulo(universidad), enBlancoEsNulo(area), enBlancoEsNulo(carrera)).stream()
                .map(OfertaAcademicaMapper::toResponse)
                .toList();
    }

    // Un parametro vacio en la URL (?carrera=) vale lo mismo que no enviarlo.
    private String enBlancoEsNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
