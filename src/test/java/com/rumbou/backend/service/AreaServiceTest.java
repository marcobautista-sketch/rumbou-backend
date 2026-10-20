package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.AreaResponse;
import com.rumbou.backend.dto.response.TemaResponse;
import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.EstructuraExamen;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.Universidad;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.AreaRepository;
import com.rumbou.backend.repository.EstructuraExamenRepository;
import com.rumbou.backend.service.impl.AreaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AreaServiceTest {

    private AreaRepository areaRepository;
    private EstructuraExamenRepository estructuraExamenRepository;
    private AreaService areaService;

    @BeforeEach
    void setUp() {
        areaRepository = mock(AreaRepository.class);
        estructuraExamenRepository = mock(EstructuraExamenRepository.class);
        areaService = new AreaServiceImpl(areaRepository, estructuraExamenRepository);
    }

    private Area areaDe(String siglas, Long id) {
        Area area = new Area(new Universidad("Universidad", siglas, 2000, 100), "B", "Ciencias Básicas");
        area.setId(id);
        return area;
    }

    @Test
    void listaLasAreasConSuUniversidadYElProximoExamen() {
        Area areaB = areaDe("UNMSM", 2L);
        areaB.setProcesoProximo("2027-II");
        areaB.setFechaProximoExamen(LocalDate.of(2027, 3, 7));
        areaB.setFechaExamenConfirmada(false);
        when(areaRepository.findAll(Sort.by("id"))).thenReturn(List.of(areaB));

        List<AreaResponse> areas = areaService.listar();

        assertThat(areas).containsExactly(new AreaResponse(
                2L, "UNMSM", "B", "Ciencias Básicas", "2027-II", LocalDate.of(2027, 3, 7), false));
    }

    @Test
    void listaLosTemasDelAreaConLosSubtemasDeSuUniversidad() {
        Tema aritmetica = new Tema("Aritmética", AreaConocimiento.MATEMATICA);
        aritmetica.setId(1L);
        aritmetica.setTemario("UNMSM: números enteros (divisibilidad, primos); razones y proporciones. "
                + "UNI (Parte I, Aritmética y Álgebra): mezcla y aleación; [Bloque] interés simple y compuesto.");
        Tema algebra = new Tema("Álgebra", AreaConocimiento.MATEMATICA);
        algebra.setId(2L);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(areaDe("UNI", 1L)));
        when(estructuraExamenRepository.findByAreaIdOrderByOrden(1L)).thenReturn(List.of(
                new EstructuraExamen(null, null, aritmetica, 10, 1),
                new EstructuraExamen(null, null, algebra, 12, 2)));

        List<TemaResponse> temas = areaService.listarTemas(1L);

        assertThat(temas).containsExactly(
                new TemaResponse(1L, "Aritmética", 10, List.of("Mezcla y aleación", "Interés simple y compuesto")),
                new TemaResponse(2L, "Álgebra", 12, List.of()));
    }

    @Test
    void unAreaQueNoExisteResponde404() {
        when(areaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> areaService.listarTemas(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
