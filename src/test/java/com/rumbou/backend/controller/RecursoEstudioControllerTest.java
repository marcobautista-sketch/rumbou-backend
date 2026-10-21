package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.LibroResponse;
import com.rumbou.backend.dto.response.LibrosTemaResponse;
import com.rumbou.backend.dto.response.VideoResponse;
import com.rumbou.backend.dto.response.VideosTemaResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.security.JwtService;
import com.rumbou.backend.service.LibroService;
import com.rumbou.backend.service.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecursoEstudioController.class)
class RecursoEstudioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

    @MockBean
    private LibroService libroService;

    private final Usuario postulante = new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);

    // JwtAuthenticationFilter se instancia igual en el slice: necesita sus dependencias aunque no se ejecute.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void devuelveLosVideosYElEnlaceDeBusquedaDelTema() throws Exception {
        given(videoService.videosDelTema(7L)).willReturn(new VideosTemaResponse(7L, "Química",
                List.of(new VideoResponse("v1", "Materia I", "Academia", "https://img/v1.jpg",
                        "https://www.youtube.com/watch?v=v1")),
                "https://www.youtube.com/results?search_query=Qu%C3%ADmica+preuniversitario"));

        mockMvc.perform(get("/api/v1/temas/7/videos")
                        .with(user(postulante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tema").value("Química"))
                .andExpect(jsonPath("$.videos[0].url").value("https://www.youtube.com/watch?v=v1"))
                .andExpect(jsonPath("$.busquedaUrl").exists());
    }

    @Test
    void devuelveLosLibrosYLaConsultaParaOpenLibrary() throws Exception {
        given(libroService.librosDelTema(7L)).willReturn(new LibrosTemaResponse(7L, "Química", "química general",
                List.of(new LibroResponse("Química general", List.of("Emilio Jimeno"), "1990", null,
                        "https://books.google.com/books?id=x"))));

        mockMvc.perform(get("/api/v1/temas/7/libros").with(user(postulante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consulta").value("química general"))
                .andExpect(jsonPath("$.libros[0].autores[0]").value("Emilio Jimeno"));
    }

    @Test
    void sinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/temas/7/videos"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(videoService, libroService);
    }
}
