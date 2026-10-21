package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.RevisionSolucionResponse;
import org.springframework.web.multipart.MultipartFile;

// El tutor de IA revisa la foto del procedimiento que el postulante resolvio en papel.
public interface RevisionSolucionService {

    RevisionSolucionResponse revisar(Long preguntaId, MultipartFile imagen);
}
