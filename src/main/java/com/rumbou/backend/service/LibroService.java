package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.LibrosTemaResponse;

// Libros de Google Books para repasar un tema, para el plan de estudio despues del simulacro.
public interface LibroService {

    LibrosTemaResponse librosDelTema(Long temaId);
}
