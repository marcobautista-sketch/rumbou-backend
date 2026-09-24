package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.CrearObjetivoRequest;
import com.rumbou.backend.dto.response.ObjetivoResponse;
import com.rumbou.backend.service.ObjetivoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Las carreras objetivo del postulante, con su PSP, su IP y el semaforo.
// Cuantos puede tener activos lo decide PlanService, no este controller.
@RestController
@RequestMapping("/api/v1/objetivos")
public class ObjetivoController {

    private final ObjetivoService objetivoService;

    public ObjetivoController(ObjetivoService objetivoService) {
        this.objetivoService = objetivoService;
    }

    @PostMapping
    public ResponseEntity<ObjetivoResponse> crear(@Valid @RequestBody CrearObjetivoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(objetivoService.crearObjetivo(request.ofertaAcademicaId()));
    }

    // El panel: con plan PRO puede traer hasta 3 objetivos y sirve para comparar universidades.
    @GetMapping
    public ResponseEntity<List<ObjetivoResponse>> listar() {
        return ResponseEntity.ok(objetivoService.listarObjetivos());
    }

    // Desactiva, no borra: si el postulante vuelve a elegir esa carrera se reutiliza la misma fila.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        objetivoService.desactivarObjetivo(id);
        return ResponseEntity.noContent().build();
    }
}
