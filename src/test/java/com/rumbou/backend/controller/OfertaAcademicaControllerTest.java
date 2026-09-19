package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.OfertaAcademicaResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.security.JwtService;
import com.rumbou.backend.service.CatalogoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OfertaAcademicaController.class)
class OfertaAcademicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogoService catalogoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private Usuario postulante() {
        return new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);
    }

    private OfertaAcademicaResponse sistemasUni() {
        return new OfertaAcademicaResponse(10L, "UNI", 4L, "GENERAL", "General", "Ingenieria de Sistemas",
                "Facultad de Ingenieria Industrial y de Sistemas", "2026-II", 1209, 31);
    }

    @Test
    void sinFiltrosDevuelveElCatalogoCompleto() throws Exception {
        given(catalogoService.buscarOfertas(null, null, null)).willReturn(List.of(sistemasUni()));

        mockMvc.perform(get("/api/v1/ofertas-academicas").with(user(postulante())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].puntajeUltimoIngresante").value(1209.0))
                // Sin areaId el cliente no podria iniciar un simulacro de esta area.
                .andExpect(jsonPath("$[0].areaId").value(4));
    }

    @Test
    void pasaLosFiltrosDeUniversidadAreaYCarreraAlServicio() throws Exception {
        given(catalogoService.buscarOfertas("UNMSM", "C", "sistemas")).willReturn(List.of());

        mockMvc.perform(get("/api/v1/ofertas-academicas?universidad=UNMSM&area=C&carrera=sistemas")
                        .with(user(postulante())))
                .andExpect(status().isOk());

        verify(catalogoService).buscarOfertas("UNMSM", "C", "sistemas");
    }
}
