package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.ResponderDesafioRequest;
import com.rumbou.backend.dto.response.DesafioDiarioResponse;
import com.rumbou.backend.service.DesafioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/desafios")
public class DesafioController {

    private final DesafioService desafioService;

    public DesafioController(DesafioService desafioService) {
        this.desafioService = desafioService;
    }

    @GetMapping("/hoy")
    public ResponseEntity<DesafioDiarioResponse> obtenerDeHoy() {
        return ResponseEntity.ok(desafioService.obtenerDeHoy());
    }

    @PostMapping("/hoy/respuesta")
    public ResponseEntity<DesafioDiarioResponse> responder(@Valid @RequestBody ResponderDesafioRequest request) {
        return ResponseEntity.ok(desafioService.responder(request.alternativaMarcada()));
    }
}
