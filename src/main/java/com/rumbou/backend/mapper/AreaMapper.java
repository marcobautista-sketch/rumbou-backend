package com.rumbou.backend.mapper;

import com.rumbou.backend.dto.response.AreaResponse;
import com.rumbou.backend.dto.response.TemaResponse;
import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.Tema;

public final class AreaMapper {

    private AreaMapper() {
    }

    public static AreaResponse toResponse(Area area) {
        return new AreaResponse(
                area.getId(), area.getUniversidad().getSiglas(), area.getCodigo(), area.getNombre());
    }

    public static TemaResponse toTemaResponse(Tema tema) {
        return new TemaResponse(tema.getId(), tema.getNombre());
    }
}
