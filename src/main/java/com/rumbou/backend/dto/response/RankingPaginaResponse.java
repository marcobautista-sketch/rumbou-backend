package com.rumbou.backend.dto.response;

import java.util.List;

public record RankingPaginaResponse(List<RankingEntradaResponse> content, long totalElements,
                                    int totalPages, int number, int size) {
}
