package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.IniciarSimulacroRequest;
import com.rumbou.backend.dto.request.ResponderPreguntaRequest;
import com.rumbou.backend.dto.response.PreguntaSimulacroResponse;
import com.rumbou.backend.dto.response.ResultadoSimulacroResponse;
import com.rumbou.backend.dto.response.SimulacroResponse;
import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.EsquemaCalificacion;
import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.EstructuraExamen;
import com.rumbou.backend.entity.Funcionalidad;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.RespuestaUsuario;
import com.rumbou.backend.entity.Simulacro;
import com.rumbou.backend.entity.TipoSimulacro;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.event.RespuestaIncorrectaEvent;
import com.rumbou.backend.event.SimulacroFinalizadoEvent;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.exception.UnauthorizedException;
import com.rumbou.backend.repository.AreaRepository;
import com.rumbou.backend.repository.EstructuraExamenRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.SimulacroRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SimulacroService {

    private final SimulacroRepository simulacroRepository;
    private final RespuestaUsuarioRepository respuestaUsuarioRepository;
    private final AreaRepository areaRepository;
    private final EstructuraExamenRepository estructuraExamenRepository;
    private final SimulacroGeneratorService simulacroGeneratorService;
    private final CalificadorService calificadorService;
    private final ApplicationEventPublisher eventPublisher;
    private final PlanService planService;

    public SimulacroService(SimulacroRepository simulacroRepository,
                             RespuestaUsuarioRepository respuestaUsuarioRepository,
                             AreaRepository areaRepository,
                             EstructuraExamenRepository estructuraExamenRepository,
                             SimulacroGeneratorService simulacroGeneratorService,
                             CalificadorService calificadorService,
                             ApplicationEventPublisher eventPublisher,
                             PlanService planService) {
        this.simulacroRepository = simulacroRepository;
        this.respuestaUsuarioRepository = respuestaUsuarioRepository;
        this.areaRepository = areaRepository;
        this.estructuraExamenRepository = estructuraExamenRepository;
        this.simulacroGeneratorService = simulacroGeneratorService;
        this.calificadorService = calificadorService;
        this.eventPublisher = eventPublisher;
        this.planService = planService;
    }

    @Transactional
    public SimulacroResponse iniciar(Usuario usuario, IniciarSimulacroRequest request) {
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el area indicada"));

        Funcionalidad funcionalidad = funcionalidadSegunTipo(request.tipo());
        planService.puedeAcceder(usuario, funcionalidad);

        Simulacro simulacro = simulacroGeneratorService.generar(
                usuario, area, request.tipo(), request.temaId());
        planService.registrarUso(usuario, funcionalidad);

        return construirRespuesta(simulacro);
    }

    @Transactional
    public void responder(Usuario usuario, Long simulacroId, ResponderPreguntaRequest request) {
        Simulacro simulacro = obtenerSimulacroDelUsuario(usuario, simulacroId);
        verificarEnCurso(simulacro);

        RespuestaUsuario respuesta = respuestaUsuarioRepository
                .findBySimulacroIdAndPreguntaId(simulacroId, request.preguntaId())
                .orElseThrow(() -> new InvalidOperationException("Esa pregunta no pertenece a este simulacro"));

        verificarAlternativaValida(request.alternativaMarcada(), respuesta.getPregunta());

        respuesta.setAlternativaMarcada(request.alternativaMarcada());
        respuestaUsuarioRepository.save(respuesta);
    }

    @Transactional
    public ResultadoSimulacroResponse finalizar(Usuario usuario, Long simulacroId) {
        Simulacro simulacro = obtenerSimulacroDelUsuario(usuario, simulacroId);
        verificarEnCurso(simulacro);

        List<RespuestaUsuario> respuestas = respuestaUsuarioRepository.findBySimulacroId(simulacroId);
        List<EstructuraExamen> estructura = estructuraExamenRepository
                .findByAreaIdOrderByOrden(simulacro.getArea().getId());

        Map<Long, EsquemaCalificacion> esquemaPorTemaId = mapearEsquemaPorTema(estructura);
        verificarQueTodoTemaTengaEsquema(respuestas, esquemaPorTemaId);

        double puntajeTotal = calificadorService.calificarSimulacro(respuestas, esquemaPorTemaId);
        respuestaUsuarioRepository.saveAll(respuestas);

        double puntajeMaximo = calcularPuntajeMaximoDelSimulacro(respuestas, esquemaPorTemaId);
        double psp = calificadorService.calcularPsp(puntajeTotal, puntajeMaximo, simulacro.getArea().getUniversidad());

        simulacro.setPuntajeObtenido(puntajeTotal);
        simulacro.setPsp(psp);
        simulacro.setEstado(EstadoSimulacro.FINALIZADO);
        simulacro.setFechaFin(LocalDateTime.now());
        simulacroRepository.save(simulacro);

        eventPublisher.publishEvent(new SimulacroFinalizadoEvent(
                simulacro.getId(), usuario.getId(), simulacro.getArea().getId(),
                simulacro.getTipo(), puntajeTotal, psp));

        for (RespuestaUsuario respuesta : respuestas) {
            if (Boolean.FALSE.equals(respuesta.getEsCorrecta())) {
                eventPublisher.publishEvent(new RespuestaIncorrectaEvent(
                        respuesta.getId(), respuesta.getPregunta().getId(), usuario.getId()));
            }
        }

        return new ResultadoSimulacroResponse(simulacro.getId(), puntajeTotal, psp, simulacro.getEstado());
    }

    // Un tema en dos bloques del area no se puede calificar (no se sabria con
    // que esquema): error claro en vez de reventar.
    private Map<Long, EsquemaCalificacion> mapearEsquemaPorTema(List<EstructuraExamen> estructura) {
        Map<Long, EsquemaCalificacion> esquemaPorTemaId = new HashMap<>();

        for (EstructuraExamen fila : estructura) {
            EsquemaCalificacion anterior = esquemaPorTemaId.put(fila.getTema().getId(), fila.getEsquema());
            if (anterior != null) {
                throw new InvalidOperationException(
                        "El area tiene el mismo tema en dos bloques de calificacion distintos: "
                                + fila.getTema().getNombre());
            }
        }
        return esquemaPorTemaId;
    }

    // La estructura del area pudo cambiar despues de generarse el simulacro.
    private void verificarQueTodoTemaTengaEsquema(List<RespuestaUsuario> respuestas,
                                                     Map<Long, EsquemaCalificacion> esquemaPorTemaId) {
        for (RespuestaUsuario respuesta : respuestas) {
            if (!esquemaPorTemaId.containsKey(respuesta.getPregunta().getTema().getId())) {
                throw new InvalidOperationException(
                        "El tema '" + respuesta.getPregunta().getTema().getNombre()
                                + "' ya no tiene esquema de calificacion en la estructura del area");
            }
        }
    }

    private double calcularPuntajeMaximoDelSimulacro(List<RespuestaUsuario> respuestas,
                                                        Map<Long, EsquemaCalificacion> esquemaPorTemaId) {
        Map<Long, Long> conteoPorTema = respuestas.stream()
                .collect(Collectors.groupingBy(r -> r.getPregunta().getTema().getId(), Collectors.counting()));

        double puntajeMaximo = 0;
        for (Map.Entry<Long, Long> entry : conteoPorTema.entrySet()) {
            EsquemaCalificacion esquema = esquemaPorTemaId.get(entry.getKey());
            puntajeMaximo += entry.getValue() * esquema.getValorAcierto();
        }
        return puntajeMaximo;
    }

    private Simulacro obtenerSimulacroDelUsuario(Usuario usuario, Long simulacroId) {
        Simulacro simulacro = simulacroRepository.findById(simulacroId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ese simulacro"));

        if (!simulacro.getUsuario().getId().equals(usuario.getId())) {
            throw new UnauthorizedException("Este simulacro no te pertenece");
        }
        return simulacro;
    }

    // null = pregunta en blanco. El rango valido sale de las alternativas de la
    // propia pregunta, no de un numero fijo.
    private void verificarAlternativaValida(Integer alternativaMarcada, Pregunta pregunta) {
        if (alternativaMarcada == null) {
            return;
        }
        if (alternativaMarcada < 0 || alternativaMarcada >= pregunta.getAlternativas().size()) {
            throw new InvalidOperationException("La alternativa marcada no existe en esta pregunta");
        }
    }

    private void verificarEnCurso(Simulacro simulacro) {
        if (simulacro.getEstado() != EstadoSimulacro.EN_CURSO) {
            throw new InvalidOperationException("Este simulacro ya fue finalizado");
        }
    }

    private SimulacroResponse construirRespuesta(Simulacro simulacro) {
        List<PreguntaSimulacroResponse> preguntas = respuestaUsuarioRepository
                .findBySimulacroId(simulacro.getId()).stream()
                .map(r -> new PreguntaSimulacroResponse(
                        r.getId(),
                        r.getPregunta().getId(),
                        r.getPregunta().getEnunciado(),
                        r.getPregunta().getAlternativas()))
                .toList();

        return new SimulacroResponse(
                simulacro.getId(), simulacro.getTipo(), simulacro.getEstado(),
                simulacro.getFechaInicio(), preguntas);
    }

    // El diagnostico gratuito se cobra con el contador del simulacro completo
    // mensual: no hay Funcionalidad.DIAGNOSTICO (un enum persistido exige migracion).
    private Funcionalidad funcionalidadSegunTipo(TipoSimulacro tipo) {
        return switch (tipo) {
            case POR_TEMA -> Funcionalidad.SIMULACRO_TEMA;
            case COMPLETO, DIAGNOSTICO -> Funcionalidad.SIMULACRO_COMPLETO;
        };
    }
}
