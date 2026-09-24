package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.SuscripcionResponse;
import com.rumbou.backend.service.SuscripcionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suscripciones")
public class SuscripcionController {

    private final SuscripcionService suscripcionService;

    public SuscripcionController(SuscripcionService suscripcionService) {
        this.suscripcionService = suscripcionService;
    }

    @PostMapping
    public ResponseEntity<SuscripcionResponse> crear() {
        return ResponseEntity.status(HttpStatus.CREATED).body(suscripcionService.crear());
    }

    @GetMapping("/me")
    public ResponseEntity<SuscripcionResponse> obtenerActual() {
        return ResponseEntity.ok(suscripcionService.obtenerActual());
    }
}
