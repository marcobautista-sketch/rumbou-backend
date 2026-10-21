package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.VideosTemaResponse;
import com.rumbou.backend.service.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/temas")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/{temaId}/videos")
    public ResponseEntity<VideosTemaResponse> videosDelTema(@PathVariable Long temaId) {
        return ResponseEntity.ok(videoService.videosDelTema(temaId));
    }
}
