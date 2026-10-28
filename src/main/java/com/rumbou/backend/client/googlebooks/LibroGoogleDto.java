package com.rumbou.backend.client.googlebooks;

import java.util.List;

public record LibroGoogleDto(
        String titulo,
        List<String> autores,
        String anio,
        String portadaUrl,
        String url
) {
}
