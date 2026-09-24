package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.OfertaAcademicaResponse;

import java.util.List;

// Lectura del catalogo academico: sin esto el postulante no tendria como saber
// el id de la oferta a la que quiere apuntar su objetivo.
public interface CatalogoService {

    List<OfertaAcademicaResponse> buscarOfertas(String universidad, String area, String carrera);
}
