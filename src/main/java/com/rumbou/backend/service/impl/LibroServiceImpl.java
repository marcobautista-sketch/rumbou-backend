package com.rumbou.backend.service.impl;

import com.rumbou.backend.client.googlebooks.GoogleBooksClient;
import com.rumbou.backend.client.googlebooks.GoogleBooksException;
import com.rumbou.backend.client.googlebooks.LibroGoogleDto;
import com.rumbou.backend.dto.response.LibroResponse;
import com.rumbou.backend.dto.response.LibrosTemaResponse;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.TemaRepository;
import com.rumbou.backend.service.LibroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LibroServiceImpl implements LibroService {

    private static final Logger log = LoggerFactory.getLogger(LibroServiceImpl.class);

    private static final int LIBROS_POR_TEMA = 3;

    // La cuota gratuita es de 1000 consultas al dia; con un dia de cache sobra.
    private static final Duration DURACION_CACHE = Duration.ofHours(24);

    // Algunos temas, buscados tal cual, traen novelas o autoayuda ("La quimica del amor",
    // "Psicologia oscura"); estos terminos dan libros de estudio.
    private static final Map<String, String> CONSULTAS = Map.ofEntries(
            Map.entry("Química", "química general"),
            Map.entry("Física", "física general"),
            Map.entry("Lenguaje", "gramática española"),
            Map.entry("Literatura", "historia de la literatura"),
            Map.entry("Psicología", "psicología general"),
            Map.entry("Geografía", "geografía del Perú"),
            Map.entry("Economía", "principios de economía"),
            Map.entry("Filosofía", "historia de la filosofía"),
            Map.entry("Geometría", "geometría plana"),
            Map.entry("Humanidades", "historia de la cultura"),
            Map.entry("Actitud", "orientación vocacional"),
            Map.entry("Habilidad Verbal", "razonamiento verbal"),
            Map.entry("Habilidad Lógico-Matemática", "razonamiento matemático")
    );

    private final TemaRepository temaRepository;
    private final GoogleBooksClient googleBooksClient;
    private final Map<Long, LibrosEnCache> cache = new ConcurrentHashMap<>();

    private record LibrosEnCache(List<LibroResponse> libros, Instant guardadoEn) {
    }

    public LibroServiceImpl(TemaRepository temaRepository, GoogleBooksClient googleBooksClient) {
        this.temaRepository = temaRepository;
        this.googleBooksClient = googleBooksClient;
    }

    @Override
    public LibrosTemaResponse librosDelTema(Long temaId) {
        Tema tema = temaRepository.findById(temaId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un tema con id " + temaId));
        String consulta = CONSULTAS.getOrDefault(tema.getNombre(), tema.getNombre());
        return new LibrosTemaResponse(tema.getId(), tema.getNombre(), consulta, buscarConCache(temaId, consulta));
    }

    // Una lista vacia no es un error: el cliente pasa a Open Library con la misma consulta.
    private List<LibroResponse> buscarConCache(Long temaId, String consulta) {
        LibrosEnCache enCache = cache.get(temaId);
        if (enCache != null && enCache.guardadoEn().plus(DURACION_CACHE).isAfter(Instant.now())) {
            return enCache.libros();
        }
        if (!googleBooksClient.estaConfigurado()) {
            return List.of();
        }
        try {
            List<LibroResponse> libros = googleBooksClient.buscarLibros(consulta, LIBROS_POR_TEMA).stream()
                    .map(LibroServiceImpl::toResponse)
                    .toList();
            if (!libros.isEmpty()) {
                cache.put(temaId, new LibrosEnCache(libros, Instant.now()));
            }
            return libros;
        } catch (GoogleBooksException ex) {
            log.warn("No se pudieron traer libros de Google Books para el tema {}: {}", temaId, ex.getMessage());
            return List.of();
        }
    }

    private static LibroResponse toResponse(LibroGoogleDto libro) {
        return new LibroResponse(libro.titulo(), libro.autores(), libro.anio(), libro.portadaUrl(), libro.url());
    }
}
