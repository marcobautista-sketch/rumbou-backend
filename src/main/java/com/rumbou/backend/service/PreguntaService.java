package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.AprobarLoteRequest;
import com.rumbou.backend.dto.request.CreatePreguntaRequest;
import com.rumbou.backend.dto.request.UpdatePreguntaRequest;
import com.rumbou.backend.dto.response.PreguntaAdminResponse;
import com.rumbou.backend.dto.response.PreguntaResponse;
import com.rumbou.backend.dto.response.TutorIaResponse;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.OrigenPregunta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PreguntaService {

    Page<PreguntaResponse> buscar(Long temaId, Dificultad dificultad, OrigenPregunta origen,
        Boolean aprobada, Pageable pageable);

    PreguntaResponse obtener(Long id);

    PreguntaAdminResponse aprobar(Long id);

    List<PreguntaAdminResponse> aprobarLote(AprobarLoteRequest request);

    PreguntaAdminResponse crear(CreatePreguntaRequest request);

    PreguntaAdminResponse actualizar(Long id, UpdatePreguntaRequest request);

    TutorIaResponse pedirExplicacionTutorIa(Long id);

    void eliminar(Long id);
}
