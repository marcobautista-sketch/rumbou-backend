package com.rumbou.backend.shared;

import com.rumbou.backend.shared.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Controlador temporal, solo para verificar que GlobalExceptionHandler funciona.
// Se elimina cuando ya existan endpoints reales que puedan lanzar excepciones.
@RestController
public class TestErrorController {

    @GetMapping("/api/test/error")
    public String triggerError() {
        throw new ResourceNotFoundException("Este es un error de prueba");
    }
}
