package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.AprobarLoteRequest;
import com.rumbou.backend.dto.request.CreatePreguntaRequest;
import com.rumbou.backend.dto.request.UpdatePreguntaRequest;
import com.rumbou.backend.dto.response.PreguntaAdminResponse;
import com.rumbou.backend.dto.response.PreguntaResponse;
import com.rumbou.backend.dto.response.TutorIaResponse;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.service.PreguntaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/preguntas")
public class PreguntaController {

    private final PreguntaService preguntaService;

    public PreguntaController(PreguntaService preguntaService) {
        this.preguntaService = preguntaService;
    }

    @GetMapping
    public ResponseEntity<Page<PreguntaResponse>> listar(@RequestParam(required = false) Long temaId,
                                                            @RequestParam(required = false) Dificultad dificultad,
                                                            @RequestParam(required = false) OrigenPregunta origen,
                                                            @RequestParam(required = false) Boolean aprobada,
                                                            Pageable pageable) {
        return ResponseEntity.ok(preguntaService.buscar(temaId, dificultad, origen, aprobada, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PreguntaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(preguntaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PreguntaAdminResponse> crear(@Valid @RequestBody CreatePreguntaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preguntaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PreguntaAdminResponse> actualizar(@PathVariable Long id,
                                                               @Valid @RequestBody UpdatePreguntaRequest request) {
        return ResponseEntity.ok(preguntaService.actualizar(id, request));
    }

    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public ResponseEntity<PreguntaAdminResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(preguntaService.aprobar(id));
    }

    @PatchMapping("/aprobar-lote")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public ResponseEntity<List<PreguntaAdminResponse>> aprobarLote(@Valid @RequestBody AprobarLoteRequest request) {
        return ResponseEntity.ok(preguntaService.aprobarLote(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        preguntaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // Sin @PreAuthorize: es un limite de plan, y eso lo decide PlanService dentro del servicio.
    @PostMapping("/{id}/tutor-ia")
    public ResponseEntity<TutorIaResponse> tutorIa(@PathVariable Long id) {
        return ResponseEntity.ok(preguntaService.pedirExplicacionTutorIa(id));
    }
}
