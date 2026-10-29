package com.rumbou.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// El historial lo guarda el cliente: el servidor no persiste conversaciones.
public record AsistenteMensajeRequest(
        @NotBlank @Size(max = 500) String mensaje,
        @Size(max = 6) List<@Valid MensajePrevioRequest> historial
) {
}
