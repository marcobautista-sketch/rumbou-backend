package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.DominioTemaResponse;
import com.rumbou.backend.dto.response.HistorialPspResponse;
import com.rumbou.backend.service.ProgresoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Panel completo del plan PRO. El chequeo del plan lo hace ProgresoService con
// PlanService: el controller no consulta la suscripcion.
@RestController
@RequestMapping("/api/v1/progreso")
public class ProgresoController {

    private final ProgresoService progresoService;

    public ProgresoController(ProgresoService progresoService) {
        this.progresoService = progresoService;
    }

    @GetMapping("/dominio-temas")
    public ResponseEntity<List<DominioTemaResponse>> dominioPorTema() {
        return ResponseEntity.ok(progresoService.dominioPorTema());
    }

    @GetMapping("/historial")
    public ResponseEntity<List<HistorialPspResponse>> historial(@RequestParam Long areaId) {
        return ResponseEntity.ok(progresoService.historialPsp(areaId));
    }
}
