package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.CrearObjetivoRequest;
import com.rumbou.backend.dto.response.ObjetivoResponse;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.service.ProgresoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private final ProgresoService progresoService;

    public ObjetivoController(ProgresoService progresoService) {
        this.progresoService = progresoService;
    }

    @PostMapping
    public ResponseEntity<ObjetivoResponse> crear(@AuthenticationPrincipal Usuario usuario,
                                                  @Valid @RequestBody CrearObjetivoRequest request) {
        ObjetivoResponse objetivo = progresoService.crearObjetivo(usuario, request.ofertaAcademicaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(objetivo);
    }

    // El panel: con plan PRO puede traer hasta 3 objetivos y sirve para comparar universidades.
    @GetMapping
    public ResponseEntity<List<ObjetivoResponse>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(progresoService.listarObjetivos(usuario));
    }

    // Desactiva, no borra: si el postulante vuelve a elegir esa carrera se reutiliza la misma fila.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@AuthenticationPrincipal Usuario usuario, @PathVariable Long id) {
        progresoService.desactivarObjetivo(usuario, id);
        return ResponseEntity.noContent().build();
    }
}
