package com.rumbou.backend.dto.response;

import java.util.List;

// consulta va siempre: si Google Books no trae nada, la web y el app buscan esa misma consulta en Open Library.
public record LibrosTemaResponse(
        Long temaId,
        String tema,
        String consulta,
        List<LibroResponse> libros
) {
}
