package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.IniciarSimulacroRequest;
import com.rumbou.backend.dto.request.ResponderPreguntaRequest;
import com.rumbou.backend.dto.response.ResultadoSimulacroResponse;
import com.rumbou.backend.dto.response.SimulacroDetalleResponse;
import com.rumbou.backend.dto.response.SimulacroResponse;
import com.rumbou.backend.dto.response.SimulacroResumenResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SimulacroService {

    SimulacroResponse iniciar(IniciarSimulacroRequest request);

    Page<SimulacroResumenResponse> listar(Pageable pageable);

    SimulacroDetalleResponse obtener(Long simulacroId);

    void responder(Long simulacroId, ResponderPreguntaRequest request);

    ResultadoSimulacroResponse finalizar(Long simulacroId);
}
