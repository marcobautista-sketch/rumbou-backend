package com.rumbou.backend.mapper;

import com.rumbou.backend.dto.response.AreaResponse;
import com.rumbou.backend.dto.response.TemaResponse;
import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.EstructuraExamen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AreaMapper {

    private static final Pattern SECCION_DEL_TEMARIO = Pattern.compile("(?:^|\\.\\s+)(UNI|UNMSM)\\b[^:]*:\\s*");

    private AreaMapper() {
    }

    public static AreaResponse toResponse(Area area) {
        return new AreaResponse(
                area.getId(),
                area.getUniversidad().getSiglas(),
                area.getCodigo(),
                area.getNombre(),
                area.getProcesoProximo(),
                area.getFechaProximoExamen(),
                area.getFechaExamenConfirmada());
    }

    public static TemaResponse toTemaResponse(EstructuraExamen estructura, String siglas) {
        return new TemaResponse(
                estructura.getTema().getId(),
                estructura.getTema().getNombre(),
                estructura.getCantidadPreguntas(),
                subtemasDe(estructura.getTema().getTemario(), siglas));
    }

    // El temario junta las secciones de cada universidad en un solo texto:
    // "UNMSM: a; b (detalle). UNI (Parte I): c; d". Solo se toman las de la universidad pedida.
    static List<String> subtemasDe(String temario, String siglas) {
        if (temario == null || temario.isBlank()) {
            return List.of();
        }
        Matcher seccion = SECCION_DEL_TEMARIO.matcher(temario);
        List<Integer> inicios = new ArrayList<>();
        List<String> universidades = new ArrayList<>();
        List<Integer> finesDeEncabezado = new ArrayList<>();
        while (seccion.find()) {
            inicios.add(seccion.start());
            universidades.add(seccion.group(1));
            finesDeEncabezado.add(seccion.end());
        }

        List<String> subtemas = new ArrayList<>();
        for (int i = 0; i < universidades.size(); i++) {
            if (!universidades.get(i).equals(siglas)) {
                continue;
            }
            int fin = i + 1 < inicios.size() ? inicios.get(i + 1) : temario.length();
            Arrays.stream(temario.substring(finesDeEncabezado.get(i), fin).split(";"))
                    .map(AreaMapper::limpiarSubtema)
                    .filter(subtema -> !subtema.isEmpty() && !subtemas.contains(subtema))
                    .forEach(subtemas::add);
        }
        return subtemas;
    }

    private static String limpiarSubtema(String texto) {
        String limpio = texto
                .replaceAll("\\[[^]]*]", "")
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("\\s+", " ")
                .replaceAll("[.\\s]+$", "")
                .trim();
        return limpio.isEmpty() ? limpio : Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
    }
}
