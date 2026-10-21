package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.VideosTemaResponse;

// Videos de YouTube para repasar un tema, para el plan de estudio despues del simulacro.
public interface VideoService {

    VideosTemaResponse videosDelTema(Long temaId);
}
