package com.rumbou.backend.service;

import com.rumbou.backend.client.gemini.GeminiClient;
import com.rumbou.backend.dto.request.AprobarLoteRequest;
import com.rumbou.backend.dto.request.CreatePreguntaRequest;
import com.rumbou.backend.dto.request.UpdatePreguntaRequest;
import com.rumbou.backend.dto.response.PreguntaAdminResponse;
import com.rumbou.backend.dto.response.PreguntaResponse;
import com.rumbou.backend.dto.response.TutorIaResponse;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.Funcionalidad;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.repository.TemaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PreguntaService {

    private final PreguntaRepository preguntaRepository;
    private final TemaRepository temaRepository;
    private final GeminiClient geminiClient;
    private final PlanService planService;

    public PreguntaService(PreguntaRepository preguntaRepository, TemaRepository temaRepository,
                            GeminiClient geminiClient, PlanService planService) {
        this.preguntaRepository = preguntaRepository;
        this.temaRepository = temaRepository;
        this.geminiClient = geminiClient;
        this.planService = planService;
    }

    // Quien no es admin solo ve aprobadas: una pregunta generada por IA no llega
    // a un postulante sin revision humana.
    public Page<PreguntaResponse> buscar(Long temaId, Dificultad dificultad, OrigenPregunta origen,
                                          Boolean aprobada, Pageable pageable, boolean esAdmin) {
        Boolean aprobadaEfectiva = esAdmin ? aprobada : Boolean.TRUE;
        return preguntaRepository.buscar(temaId, dificultad, origen, aprobadaEfectiva, pageable)
                .map(this::aResponse);
    }

    public PreguntaResponse obtener(Long id, boolean esAdmin) {
        Pregunta pregunta = obtenerEntidad(id);
        if (!esAdmin && !pregunta.isAprobada()) {
            throw new ResourceNotFoundException("No existe una pregunta con id " + id);
        }
        return aResponse(pregunta);
    }

    @Transactional
    public PreguntaAdminResponse aprobar(Long id) {
        Pregunta pregunta = obtenerEntidad(id);
        pregunta.setAprobada(true);
        return aAdminResponse(pregunta);
    }

    // Todo el lote en una transaccion: un id inexistente revierte el lote completo.
    @Transactional
    public List<PreguntaAdminResponse> aprobarLote(AprobarLoteRequest request) {
        List<PreguntaAdminResponse> aprobadas = new ArrayList<>();
        for (Long id : request.ids()) {
            Pregunta pregunta = obtenerEntidad(id);
            pregunta.setAprobada(true);
            aprobadas.add(aAdminResponse(pregunta));
        }
        return aprobadas;
    }

    @Transactional
    public PreguntaAdminResponse crear(CreatePreguntaRequest request) {
        Tema tema = obtenerTema(request.temaId());

        Pregunta pregunta = new Pregunta(
                tema,
                request.enunciado(),
                request.alternativas(),
                request.claveCorrecta(),
                request.explicacion(),
                request.dificultad(),
                request.origen(),
                request.aprobada()
        );

        return aAdminResponse(preguntaRepository.save(pregunta));
    }

    @Transactional
    public PreguntaAdminResponse actualizar(Long id, UpdatePreguntaRequest request) {
        Pregunta pregunta = obtenerEntidad(id);
        Tema tema = obtenerTema(request.temaId());

        pregunta.setTema(tema);
        pregunta.setEnunciado(request.enunciado());
        pregunta.setAlternativas(request.alternativas());
        pregunta.setClaveCorrecta(request.claveCorrecta());
        pregunta.setExplicacion(request.explicacion());
        pregunta.setDificultad(request.dificultad());
        pregunta.setOrigen(request.origen());
        pregunta.setAprobada(request.aprobada());

        return aAdminResponse(pregunta);
    }

    // Consulta PRO real: se pregunta a Gemini en el momento y se descuenta del cupo
    // diario, sin cachear (la explicacion estatica la cachea TutorIaExplicacionListener).
    public TutorIaResponse pedirExplicacionTutorIa(Long id, Usuario usuario, boolean esAdmin) {
        Pregunta pregunta = obtenerEntidad(id);
        if (!esAdmin && !pregunta.isAprobada()) {
            throw new ResourceNotFoundException("No existe una pregunta con id " + id);
        }
        planService.puedeAcceder(usuario, Funcionalidad.TUTOR_IA);

        String explicacion = geminiClient.explicar(
                pregunta.getEnunciado(), pregunta.getAlternativas(), pregunta.getClaveCorrecta());

        planService.registrarUso(usuario, Funcionalidad.TUTOR_IA);
        return new TutorIaResponse(pregunta.getId(), explicacion);
    }

    @Transactional
    public void eliminar(Long id) {
        Pregunta pregunta = obtenerEntidad(id);
        preguntaRepository.delete(pregunta);
    }

    private Pregunta obtenerEntidad(Long id) {
        return preguntaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una pregunta con id " + id));
    }

    private Tema obtenerTema(Long temaId) {
        return temaRepository.findById(temaId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un tema con id " + temaId));
    }

    private PreguntaResponse aResponse(Pregunta pregunta) {
        return new PreguntaResponse(
                pregunta.getId(),
                pregunta.getTema().getId(),
                pregunta.getTema().getNombre(),
                pregunta.getEnunciado(),
                pregunta.getAlternativas(),
                pregunta.getDificultad()
        );
    }

    private PreguntaAdminResponse aAdminResponse(Pregunta pregunta) {
        return new PreguntaAdminResponse(
                pregunta.getId(),
                pregunta.getTema().getId(),
                pregunta.getTema().getNombre(),
                pregunta.getEnunciado(),
                pregunta.getAlternativas(),
                pregunta.getClaveCorrecta(),
                pregunta.getExplicacion(),
                pregunta.getDificultad(),
                pregunta.getOrigen(),
                pregunta.isAprobada()
        );
    }
}
