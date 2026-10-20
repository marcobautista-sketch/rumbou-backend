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

import java.util.List;

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

    @Test
    void listaLasAreasConLaSiglaDeSuUniversidad() {
        Universidad unmsm = new Universidad("Universidad Nacional Mayor de San Marcos", "UNMSM", 2000, 100);
        Area areaB = new Area(unmsm, "B", "Ciencias de la Salud");
        areaB.setId(2L);
        when(areaRepository.findAll(Sort.by("id"))).thenReturn(List.of(areaB));

        List<AreaResponse> areas = areaService.listar();

        assertThat(areas).containsExactly(new AreaResponse(2L, "UNMSM", "B", "Ciencias de la Salud"));
    }

    @Test
    void listaLosTemasDelAreaEnElOrdenDelExamen() {
        Tema aritmetica = new Tema("Aritmetica", AreaConocimiento.MATEMATICA);
        aritmetica.setId(1L);
        Tema algebra = new Tema("Algebra", AreaConocimiento.MATEMATICA);
        algebra.setId(2L);
        when(areaRepository.existsById(1L)).thenReturn(true);
        when(estructuraExamenRepository.findByAreaIdOrderByOrden(1L)).thenReturn(List.of(
                new EstructuraExamen(null, null, aritmetica, 10, 1),
                new EstructuraExamen(null, null, algebra, 10, 2)));

        List<TemaResponse> temas = areaService.listarTemas(1L);

        assertThat(temas).containsExactly(new TemaResponse(1L, "Aritmetica"), new TemaResponse(2L, "Algebra"));
    }

    @Test
    void unAreaQueNoExisteResponde404() {
        when(areaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> areaService.listarTemas(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
