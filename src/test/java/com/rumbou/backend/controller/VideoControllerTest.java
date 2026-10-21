package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.VideoResponse;
import com.rumbou.backend.dto.response.VideosTemaResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.security.JwtService;
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

@WebMvcTest(VideoController.class)
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

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
                        .with(user(new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tema").value("Química"))
                .andExpect(jsonPath("$.videos[0].url").value("https://www.youtube.com/watch?v=v1"))
                .andExpect(jsonPath("$.busquedaUrl").exists());
    }

    @Test
    void sinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/temas/7/videos"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(videoService);
    }
}
