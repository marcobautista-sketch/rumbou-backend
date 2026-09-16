package com.rumbou.backend.contenido;

import com.rumbou.backend.academico.Tema;
import com.rumbou.backend.academico.TemaRepository;
import com.rumbou.backend.auth.Usuario;
import com.rumbou.backend.contenido.dto.CreatePreguntaRequest;
import com.rumbou.backend.contenido.dto.PreguntaAdminResponse;
import com.rumbou.backend.contenido.dto.PreguntaResponse;
import com.rumbou.backend.contenido.dto.TutorIaResponse;
import com.rumbou.backend.contenido.dto.UpdatePreguntaRequest;
import com.rumbou.backend.contenido.gemini.GeminiClient;
import com.rumbou.backend.shared.exception.ResourceNotFoundException;
import com.rumbou.backend.suscripcion.Funcionalidad;
import com.rumbou.backend.suscripcion.PlanService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // esAdmin fuerza aprobada=true para cualquiera que no sea administrador: un
    // postulante no debe poder ver preguntas generadas por IA que todavia no
    // pasaron revision humana.
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

    // A diferencia de la explicacion estatica que cachea TutorIaExplicacionListener,
    // esta es la consulta PRO real: se le pregunta a Gemini en el momento y se
    // descuenta del cupo diario del usuario, sin cachear la respuesta en la Pregunta.
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
