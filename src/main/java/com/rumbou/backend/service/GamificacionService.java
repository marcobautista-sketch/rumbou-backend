package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.GamificacionResponse;

public interface GamificacionService {

    GamificacionResponse obtenerResumen();

    void procesarSimulacroFinalizado(Long usuarioId);
}
