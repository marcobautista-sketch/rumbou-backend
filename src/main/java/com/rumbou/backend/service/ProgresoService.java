package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.DominioTemaResponse;
import com.rumbou.backend.dto.response.HistorialPspResponse;

import java.util.List;

// Panel de analitica del plan PRO: dominio por tema e historial de PSP.
public interface ProgresoService {

    List<DominioTemaResponse> dominioPorTema();

    List<HistorialPspResponse> historialPsp(Long areaId);
}
