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
import com.rumbou.backend.exception.ResourceInUseException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.mapper.PreguntaMapper;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.TemaRepository;
import com.rumbou.backend.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PreguntaService {

    private static final int TAMANO_PAGINA_MAXIMO = 50;

    private final PreguntaRepository preguntaRepository;
    private final TemaRepository temaRepository;
    private final RespuestaUsuarioRepository respuestaUsuarioRepository;
    private final GeminiClient geminiClient;
    private final PlanService planService;
    private final CurrentUserService currentUserService;

    public PreguntaService(PreguntaRepository preguntaRepository, TemaRepository temaRepository,
                            RespuestaUsuarioRepository respuestaUsuarioRepository, GeminiClient geminiClient,
                            PlanService planService, CurrentUserService currentUserService) {
        this.preguntaRepository = preguntaRepository;
        this.temaRepository = temaRepository;
        this.respuestaUsuarioRepository = respuestaUsuarioRepository;
        this.geminiClient = geminiClient;
        this.planService = planService;
        this.currentUserService = currentUserService;
    }

    // Quien no es admin solo ve aprobadas: una pregunta generada por IA no llega
    // a un postulante sin revision humana.
    @Transactional(readOnly = true)
    public Page<PreguntaResponse> buscar(Long temaId, Dificultad dificultad, OrigenPregunta origen,
                                          Boolean aprobada, Pageable pageable) {
        Boolean aprobadaEfectiva = currentUserService.esAdmin() ? aprobada : Boolean.TRUE;
        return preguntaRepository.buscar(temaId, dificultad, origen, aprobadaEfectiva, limitarTamano(pageable))
                .map(PreguntaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PreguntaResponse obtener(Long id) {
        return PreguntaMapper.toResponse(obtenerVisibleParaElUsuario(id));
    }

    @Transactional
    public PreguntaAdminResponse aprobar(Long id) {
        Pregunta pregunta = obtenerEntidad(id);
        pregunta.setAprobada(true);
        return PreguntaMapper.toAdminResponse(pregunta);
    }

    // Todo el lote en una transaccion: un id inexistente revierte el lote completo.
    @Transactional
    public List<PreguntaAdminResponse> aprobarLote(AprobarLoteRequest request) {
        return request.ids().stream()
                .map(this::aprobar)
                .toList();
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

        return PreguntaMapper.toAdminResponse(preguntaRepository.save(pregunta));
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

        return PreguntaMapper.toAdminResponse(pregunta);
    }

    // Consulta PRO real: se pregunta a Gemini en el momento y se descuenta del cupo
    // diario, sin cachear (la explicacion estatica la cachea TutorIaExplicacionListener).
    public TutorIaResponse pedirExplicacionTutorIa(Long id) {
        Pregunta pregunta = obtenerVisibleParaElUsuario(id);
        planService.puedeAcceder(currentUserService.getUsuario(), Funcionalidad.TUTOR_IA);

        String explicacion = geminiClient.explicar(
                pregunta.getEnunciado(), pregunta.getAlternativas(), pregunta.getClaveCorrecta());

        planService.registrarUso(currentUserService.getUsuario(), Funcionalidad.TUTOR_IA);
        return new TutorIaResponse(pregunta.getId(), explicacion);
    }

    // Una pregunta que ya salio en algun simulacro no se borra: se perderia el
    // historial de respuestas de los postulantes. Se puede desaprobar con PUT.
    @Transactional
    public void eliminar(Long id) {
        Pregunta pregunta = obtenerEntidad(id);
        if (respuestaUsuarioRepository.existsByPreguntaId(id)) {
            throw new ResourceInUseException(
                    "La pregunta ya fue usada en simulacros; desapruebala en lugar de eliminarla");
        }
        preguntaRepository.delete(pregunta);
    }

    // Para un postulante, una pregunta sin aprobar no existe.
    private Pregunta obtenerVisibleParaElUsuario(Long id) {
        Pregunta pregunta = obtenerEntidad(id);
        if (!pregunta.isAprobada() && !currentUserService.esAdmin()) {
            throw new ResourceNotFoundException("No existe una pregunta con id " + id);
        }
        return pregunta;
    }

    private Pregunta obtenerEntidad(Long id) {
        return preguntaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una pregunta con id " + id));
    }

    private Tema obtenerTema(Long temaId) {
        return temaRepository.findById(temaId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un tema con id " + temaId));
    }

    private Pageable limitarTamano(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return PageRequest.of(0, TAMANO_PAGINA_MAXIMO, pageable.getSort());
        }
        if (pageable.getPageSize() <= TAMANO_PAGINA_MAXIMO) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), TAMANO_PAGINA_MAXIMO, pageable.getSort());
    }
}
