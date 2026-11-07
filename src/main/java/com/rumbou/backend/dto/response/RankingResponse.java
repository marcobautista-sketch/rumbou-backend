package com.rumbou.backend.dto.response;

import java.time.LocalDate;

public record RankingResponse(String universidad, LocalDate semanaInicio, boolean participa, String alias,
                              Long miPosicion, int miXpSemanal, RankingPaginaResponse entradas) {
}
