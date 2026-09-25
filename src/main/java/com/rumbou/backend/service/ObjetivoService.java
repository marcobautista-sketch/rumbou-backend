package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.ObjetivoResponse;

import java.util.List;

// Las carreras objetivo del postulante con su ultimo PSP e IP. Cuantos objetivos
// puede tener activos lo decide PlanService.
public interface ObjetivoService {

    ObjetivoResponse crearObjetivo(Long ofertaAcademicaId);

    List<ObjetivoResponse> listarObjetivos();

    void desactivarObjetivo(Long objetivoId);

    void actualizarTrasSimulacro(Long usuarioId, Long areaId, double psp);
}
