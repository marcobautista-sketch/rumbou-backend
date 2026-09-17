package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.IniciarSimulacroRequest;
import com.rumbou.backend.dto.request.ResponderPreguntaRequest;
import com.rumbou.backend.dto.response.ResultadoSimulacroResponse;
import com.rumbou.backend.dto.response.SimulacroResponse;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.service.SimulacroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<SimulacroResponse> iniciar(@AuthenticationPrincipal Usuario usuario,
                                                        @Valid @RequestBody IniciarSimulacroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(simulacroService.iniciar(usuario, request));
    }

    @PostMapping("/{id}/respuestas")
    public ResponseEntity<Void> responder(@AuthenticationPrincipal Usuario usuario,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ResponderPreguntaRequest request) {
        simulacroService.responder(usuario, id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<ResultadoSimulacroResponse> finalizar(@AuthenticationPrincipal Usuario usuario,
                                                                   @PathVariable Long id) {
        return ResponseEntity.ok(simulacroService.finalizar(usuario, id));
    }
}
