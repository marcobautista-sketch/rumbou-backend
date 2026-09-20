package com.rumbou.backend.dto.request;

import com.rumbou.backend.entity.TipoSimulacro;
import jakarta.validation.constraints.NotNull;

// temaId solo se usa cuando el tipo es POR_TEMA; en los demas se ignora.
public record IniciarSimulacroRequest(
        @NotNull Long areaId,
        @NotNull TipoSimulacro tipo,
        Long temaId
) {
}
