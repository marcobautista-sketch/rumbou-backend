package com.rumbou.backend.dto.response;

import java.util.List;

// busquedaUrl siempre llega: si no hay videos (sin clave o sin cuota), el plan de estudio usa ese enlace.
public record VideosTemaResponse(
        Long temaId,
        String tema,
        List<VideoResponse> videos,
        String busquedaUrl
) {
}
