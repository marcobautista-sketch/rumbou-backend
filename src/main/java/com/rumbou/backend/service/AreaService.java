package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.AreaResponse;
import com.rumbou.backend.dto.response.TemaResponse;

import java.util.List;

// Lo que el cliente necesita para armar un simulacro: el areaId y, si es POR_TEMA, el temaId.
public interface AreaService {
    List<AreaResponse> listar();

    List<TemaResponse> listarTemas(Long areaId);
}
