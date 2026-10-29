package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.MensajePrevioRequest;
import com.rumbou.backend.dto.response.AsistenteResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Asistente de estudio que conoce el progreso del postulante; se le habla por el microfono o se le escribe.
public interface AsistenteService {

    AsistenteResponse responderTexto(String mensaje, List<MensajePrevioRequest> historial);

    AsistenteResponse responderVoz(MultipartFile audio, String historialJson);
}
