package com.rumbou.backend.dto.response;

import java.time.LocalDate;
import java.util.List;

public record GamificacionResponse(
        int xpTotal,
        int xpSemanal,
        int rachaActual,
        int rachaMaxima,
        LocalDate ultimaActividad,
        List<LogroResponse> logros
) {
}
