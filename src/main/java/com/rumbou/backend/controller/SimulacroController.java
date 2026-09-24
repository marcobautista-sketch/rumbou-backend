package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.IniciarSimulacroRequest;
import com.rumbou.backend.dto.request.ResponderPreguntaRequest;
import com.rumbou.backend.dto.response.ResultadoSimulacroResponse;
import com.rumbou.backend.dto.response.SimulacroDetalleResponse;
import com.rumbou.backend.dto.response.SimulacroResponse;
import com.rumbou.backend.dto.response.SimulacroResumenResponse;
import com.rumbou.backend.service.SimulacroService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulacros")
public class SimulacroController {

    private final SimulacroService simulacroService;

    public SimulacroController(SimulacroService simulacroService) {
        this.simulacroService = simulacroService;
    }

    @PostMapping
    public ResponseEntity<SimulacroResponse> iniciar(@Valid @RequestBody IniciarSimulacroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(simulacroService.iniciar(request));
    }

    // Los simulacros del usuario autenticado, del mas reciente al mas antiguo.
    @GetMapping
    public ResponseEntity<Page<SimulacroResumenResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(simulacroService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SimulacroDetalleResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(simulacroService.obtener(id));
    }

    @PostMapping("/{id}/respuestas")
    public ResponseEntity<Void> responder(@PathVariable Long id,
                                            @Valid @RequestBody ResponderPreguntaRequest request) {
        simulacroService.responder(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<ResultadoSimulacroResponse> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(simulacroService.finalizar(id));
    }
}
