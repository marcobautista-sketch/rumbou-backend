package com.rumbou.backend.dto.response;

public record RankingEntradaResponse(long posicion, String alias, int xpSemanal, boolean esUsuarioActual) {
}
