package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.LibrosTemaResponse;
import com.rumbou.backend.dto.response.VideosTemaResponse;
import com.rumbou.backend.service.LibroService;
import com.rumbou.backend.service.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Material para repasar un tema (plan de estudio). Las claves de Google quedan en el servidor.
@RestController
@RequestMapping("/api/v1/temas")
public class RecursoEstudioController {

    private final VideoService videoService;
    private final LibroService libroService;

    public RecursoEstudioController(VideoService videoService, LibroService libroService) {
        this.videoService = videoService;
        this.libroService = libroService;
    }

    @GetMapping("/{temaId}/videos")
    public ResponseEntity<VideosTemaResponse> videosDelTema(@PathVariable Long temaId) {
        return ResponseEntity.ok(videoService.videosDelTema(temaId));
    }

    @GetMapping("/{temaId}/libros")
    public ResponseEntity<LibrosTemaResponse> librosDelTema(@PathVariable Long temaId) {
        return ResponseEntity.ok(libroService.librosDelTema(temaId));
    }
}
