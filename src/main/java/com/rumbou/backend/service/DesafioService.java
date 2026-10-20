package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.DesafioDiarioResponse;

// Una pregunta al dia del tema mas debil del postulante, con XP y bono semanal.
public interface DesafioService {

    DesafioDiarioResponse obtenerDeHoy();

    DesafioDiarioResponse responder(int alternativaMarcada);
}
