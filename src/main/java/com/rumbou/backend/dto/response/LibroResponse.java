package com.rumbou.backend.dto.response;

import java.util.List;

public record LibroResponse(
        String titulo,
        List<String> autores,
        String anio,
        String portadaUrl,
        String url
) {
}
