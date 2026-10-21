package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.RevisionSolucionResponse;
import com.rumbou.backend.service.RevisionSolucionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/preguntas")
public class RevisionSolucionController {

    private final RevisionSolucionService revisionSolucionService;

    public RevisionSolucionController(RevisionSolucionService revisionSolucionService) {
        this.revisionSolucionService = revisionSolucionService;
    }

    // La foto llega como multipart/form-data en la parte "imagen" (camara o pizarra del app).
    // Sin @PreAuthorize: como /tutor-ia, es un limite de plan y lo decide PlanService.
    @PostMapping(value = "/{id}/revisar-solucion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RevisionSolucionResponse> revisar(@PathVariable Long id,
                                                            @RequestPart("imagen") MultipartFile imagen) {
        return ResponseEntity.ok(revisionSolucionService.revisar(id, imagen));
    }
}
