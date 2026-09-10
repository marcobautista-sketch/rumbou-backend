package com.rumbou.backend.examen.dto;

import com.rumbou.backend.examen.TipoSimulacro;
import jakarta.validation.constraints.NotNull;

public record IniciarSimulacroRequest(
        @NotNull Long areaId,
        @NotNull TipoSimulacro tipo
) {
}
