package com.rumbou.backend.service;

import com.rumbou.backend.client.googlebooks.GoogleBooksClient;
import com.rumbou.backend.client.googlebooks.GoogleBooksException;
import com.rumbou.backend.client.googlebooks.LibroGoogleDto;
import com.rumbou.backend.dto.response.LibrosTemaResponse;
import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.TemaRepository;
import com.rumbou.backend.service.impl.LibroServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LibroServiceTest {

    @Mock
    private TemaRepository temaRepository;

    @Mock
    private GoogleBooksClient googleBooksClient;

    private LibroServiceImpl libroService;

    private final LibroGoogleDto baldor = new LibroGoogleDto("Aritmética", List.of("Aurelio Baldor"), "1983",
            null, "https://books.google.com/books?id=abc");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        libroService = new LibroServiceImpl(temaRepository, googleBooksClient);
        given(temaRepository.findById(1L)).willReturn(Optional.of(tema(1L, "Aritmética")));
        given(temaRepository.findById(2L)).willReturn(Optional.of(tema(2L, "Química")));
        given(googleBooksClient.estaConfigurado()).willReturn(true);
    }

    private Tema tema(Long id, String nombre) {
        Tema tema = new Tema(nombre, AreaConocimiento.MATEMATICA);
        tema.setId(id);
        return tema;
    }

    @Test
    void unTemaSinTerminoEspecialSeBuscaPorSuNombre() {
        given(googleBooksClient.buscarLibros("Aritmética", 3)).willReturn(List.of(baldor));

        LibrosTemaResponse respuesta = libroService.librosDelTema(1L);

        assertThat(respuesta.consulta()).isEqualTo("Aritmética");
        assertThat(respuesta.libros()).extracting("titulo").containsExactly("Aritmética");
    }

    @Test
    void algunosTemasUsanTerminosQueEvitanNovelasYAutoayuda() {
        given(googleBooksClient.buscarLibros(anyString(), anyInt())).willReturn(List.of());

        assertThat(libroService.librosDelTema(2L).consulta()).isEqualTo("química general");
        verify(googleBooksClient).buscarLibros("química general", 3);
    }

    @Test
    void laSegundaConsultaDelMismoTemaSaleDeLaCache() {
        given(googleBooksClient.buscarLibros(anyString(), anyInt())).willReturn(List.of(baldor));

        libroService.librosDelTema(1L);
        libroService.librosDelTema(1L);

        verify(googleBooksClient, times(1)).buscarLibros(anyString(), anyInt());
    }

    @Test
    void siGoogleBooksFallaDevuelveLaConsultaSinLibrosParaUsarOpenLibrary() {
        given(googleBooksClient.buscarLibros(anyString(), anyInt()))
                .willThrow(new GoogleBooksException("Google Books respondio 503"));

        LibrosTemaResponse respuesta = libroService.librosDelTema(2L);

        assertThat(respuesta.libros()).isEmpty();
        assertThat(respuesta.consulta()).isEqualTo("química general");
    }

    @Test
    void sinClaveNiSeIntentaGoogleBooks() {
        given(googleBooksClient.estaConfigurado()).willReturn(false);

        assertThat(libroService.librosDelTema(1L).libros()).isEmpty();
        verify(googleBooksClient, never()).buscarLibros(anyString(), anyInt());
    }

    @Test
    void unTemaInexistenteResponde404() {
        assertThatThrownBy(() -> libroService.librosDelTema(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
