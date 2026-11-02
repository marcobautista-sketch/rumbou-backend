package com.rumbou.backend.dto.response;

import java.util.List;

// Un paso de la resolucion: titulo corto, detalle con LaTeX y una tabla opcional
// (lista de filas; la primera es el encabezado). Revela la respuesta, igual que la explicacion.
public record PasoResolucionResponse(
        String titulo,
        String detalle,
        List<List<String>> tabla
) {
}
