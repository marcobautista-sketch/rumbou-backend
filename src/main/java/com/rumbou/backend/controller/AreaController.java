package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.AreaResponse;
import com.rumbou.backend.dto.response.TemaResponse;
import com.rumbou.backend.service.AreaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/areas")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @GetMapping
    public ResponseEntity<List<AreaResponse>> listar() {
        return ResponseEntity.ok(areaService.listar());
    }

    @GetMapping("/{id}/temas")
    public ResponseEntity<List<TemaResponse>> listarTemas(@PathVariable Long id) {
        return ResponseEntity.ok(areaService.listarTemas(id));
    }
}
