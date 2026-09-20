package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.OfertaAcademicaResponse;
import com.rumbou.backend.service.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ofertas-academicas")
public class OfertaAcademicaController {

    private final CatalogoService catalogoService;

    public OfertaAcademicaController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    // Los tres filtros son opcionales: sin ninguno devuelve el catalogo completo.
    @GetMapping
    public ResponseEntity<List<OfertaAcademicaResponse>> buscar(
            @RequestParam(required = false) String universidad,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String carrera) {
        return ResponseEntity.ok(catalogoService.buscarOfertas(universidad, area, carrera));
    }
}
