package com.rumbou.backend.dto.response;

import java.time.LocalDateTime;

// ultimoPsp, indicePreparacion, estado y estadoDescripcion son null hasta que el
// usuario finaliza un simulacro en el area de la oferta.
public record ObjetivoResponse(
        Long id,
        Long ofertaAcademicaId,
        String universidad,
        // Con el areaId el panel puede pedir directamente el historial de PSP de este objetivo.
        Long areaId,
        String area,
        String carrera,
        String procesoAdmision,
        double puntajeUltimoIngresante,
        Double ultimoPsp,
        Double indicePreparacion,
        EstadoPreparacion estado,
        String estadoDescripcion,
        LocalDateTime fechaActualizacion
) {
}
