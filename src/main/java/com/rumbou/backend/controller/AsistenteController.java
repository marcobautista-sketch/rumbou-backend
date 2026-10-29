package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.AsistenteMensajeRequest;
import com.rumbou.backend.dto.request.MensajePrevioRequest;
import com.rumbou.backend.dto.response.AsistenteResponse;
import com.rumbou.backend.service.AsistenteService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Sin @PreAuthorize: como /tutor-ia, es un limite de plan y lo decide PlanService.
@RestController
@RequestMapping("/api/v1/asistente")
public class AsistenteController {

    private final AsistenteService asistenteService;

    public AsistenteController(AsistenteService asistenteService) {
        this.asistenteService = asistenteService;
    }

    // Respaldo del microfono: la misma conversacion, escrita.
    @PostMapping("/mensajes")
    public ResponseEntity<AsistenteResponse> responderTexto(@Valid @RequestBody AsistenteMensajeRequest request) {
        List<MensajePrevioRequest> historial =
                request.historial() == null ? List.of() : request.historial();
        return ResponseEntity.ok(asistenteService.responderTexto(request.mensaje(), historial));
    }

    // El audio llega en la parte "audio"; el historial, como texto JSON en el campo "historial".
    @PostMapping(value = "/voz", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AsistenteResponse> responderVoz(@RequestPart("audio") MultipartFile audio,
                                                          @RequestParam(value = "historial", required = false)
                                                          String historial) {
        return ResponseEntity.ok(asistenteService.responderVoz(audio, historial));
    }
}
