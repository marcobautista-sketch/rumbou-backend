package com.rumbou.backend.dto.request;

import com.rumbou.backend.entity.TipoSimulacro;
import jakarta.validation.constraints.NotNull;

public record IniciarSimulacroRequest(
        @NotNull Long areaId,
        @NotNull TipoSimulacro tipo
) {
}
