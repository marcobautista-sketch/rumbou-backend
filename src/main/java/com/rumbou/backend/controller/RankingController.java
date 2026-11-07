package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.ParticipacionRankingRequest;
import com.rumbou.backend.dto.response.RankingResponse;
import com.rumbou.backend.service.RankingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ranking")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public ResponseEntity<RankingResponse> obtener(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(rankingService.obtenerRanking(page, size));
    }

    @PutMapping("/participacion")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RankingResponse> participar(@Valid @RequestBody ParticipacionRankingRequest request) {
        return ResponseEntity.ok(rankingService.participar(request));
    }

    @DeleteMapping("/participacion")
    public ResponseEntity<Void> dejarDeParticipar() {
        rankingService.dejarDeParticipar();
        return ResponseEntity.noContent().build();
    }
}
