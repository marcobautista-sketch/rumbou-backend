package com.rumbou.backend.service.impl;

import com.rumbou.backend.dto.request.IniciarSimulacroRequest;
import com.rumbou.backend.dto.request.ResponderPreguntaRequest;
import com.rumbou.backend.dto.response.ResultadoSimulacroResponse;
import com.rumbou.backend.dto.response.SimulacroDetalleResponse;
import com.rumbou.backend.dto.response.SimulacroResponse;
import com.rumbou.backend.dto.response.SimulacroResumenResponse;
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
import com.rumbou.backend.exception.ForbiddenException;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.mapper.SimulacroMapper;
import com.rumbou.backend.repository.AreaRepository;
import com.rumbou.backend.repository.EstructuraExamenRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.SimulacroRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.CalificadorService;
import com.rumbou.backend.service.PlanService;
import com.rumbou.backend.service.SimulacroGeneratorService;
import com.rumbou.backend.service.SimulacroService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SimulacroServiceImpl implements SimulacroService {

    private final SimulacroRepository simulacroRepository;
    private final RespuestaUsuarioRepository respuestaUsuarioRepository;
    private final AreaRepository areaRepository;
    private final EstructuraExamenRepository estructuraExamenRepository;
    private final SimulacroGeneratorService simulacroGeneratorService;
    private final CalificadorService calificadorService;
    private final ApplicationEventPublisher eventPublisher;
    private final PlanService planService;
    private final CurrentUserService currentUserService;

    public SimulacroServiceImpl(SimulacroRepository simulacroRepository,
                                 RespuestaUsuarioRepository respuestaUsuarioRepository,
                                 AreaRepository areaRepository,
                                 EstructuraExamenRepository estructuraExamenRepository,
                                 SimulacroGeneratorService simulacroGeneratorService,
                                 CalificadorService calificadorService,
                                 ApplicationEventPublisher eventPublisher,
                                 PlanService planService,
                                 CurrentUserService currentUserService) {
        this.simulacroRepository = simulacroRepository;
        this.respuestaUsuarioRepository = respuestaUsuarioRepository;
        this.areaRepository = areaRepository;
        this.estructuraExamenRepository = estructuraExamenRepository;
        this.simulacroGeneratorService = simulacroGeneratorService;
        this.calificadorService = calificadorService;
        this.eventPublisher = eventPublisher;
        this.planService = planService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    @Override
    public SimulacroResponse iniciar(IniciarSimulacroRequest request) {
        Usuario usuario = currentUserService.getUsuario();
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el area indicada"));

        Funcionalidad funcionalidad = funcionalidadSegunTipo(request.tipo());
        planService.puedeAcceder(usuario, funcionalidad);

        Simulacro simulacro = simulacroGeneratorService.generar(
                usuario, area, request.tipo(), request.temaId());
        planService.registrarUso(usuario, funcionalidad);

        return SimulacroMapper.toResponse(simulacro, respuestaUsuarioRepository.findBySimulacroId(simulacro.getId()));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<SimulacroResumenResponse> listar(Pageable pageable) {
        return simulacroRepository
                .findByUsuarioIdOrderByFechaInicioDesc(currentUserService.getUsuarioId(), pageable)
                .map(SimulacroMapper::toResumen);
    }

    // En curso: las preguntas sin clave. Finalizado: la correccion completa con explicaciones.
    @Transactional(readOnly = true)
    @Override
    public SimulacroDetalleResponse obtener(Long simulacroId) {
        Simulacro simulacro = obtenerSimulacroDelUsuario(simulacroId);
        return SimulacroMapper.toDetalle(simulacro, respuestaUsuarioRepository.findBySimulacroId(simulacroId));
    }

    @Transactional
    @Override
    public void responder(Long simulacroId, ResponderPreguntaRequest request) {
        Simulacro simulacro = obtenerSimulacroDelUsuario(simulacroId);
        verificarEnCurso(simulacro);

        RespuestaUsuario respuesta = respuestaUsuarioRepository
                .findBySimulacroIdAndPreguntaId(simulacroId, request.preguntaId())
                .orElseThrow(() -> new InvalidOperationException("Esa pregunta no pertenece a este simulacro"));

        verificarAlternativaValida(request.alternativaMarcada(), respuesta.getPregunta());

        respuesta.setAlternativaMarcada(request.alternativaMarcada());
        respuestaUsuarioRepository.save(respuesta);
    }

    @Transactional
    @Override
    public ResultadoSimulacroResponse finalizar(Long simulacroId) {
        Simulacro simulacro = obtenerSimulacroDelUsuario(simulacroId);
        verificarEnCurso(simulacro);

        List<RespuestaUsuario> respuestas = respuestaUsuarioRepository.findBySimulacroId(simulacroId);
        calificar(simulacro, respuestas);
        simulacroRepository.save(simulacro);

        publicarEventos(simulacro, respuestas);
        return SimulacroMapper.toResultado(simulacro);
    }

    private void calificar(Simulacro simulacro, List<RespuestaUsuario> respuestas) {
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
    }

    private void publicarEventos(Simulacro simulacro, List<RespuestaUsuario> respuestas) {
        Long usuarioId = simulacro.getUsuario().getId();
        eventPublisher.publishEvent(new SimulacroFinalizadoEvent(
                simulacro.getId(), usuarioId, simulacro.getArea().getId(),
                simulacro.getTipo(), simulacro.getPuntajeObtenido(), simulacro.getPsp()));

        respuestas.stream()
                .filter(respuesta -> Boolean.FALSE.equals(respuesta.getEsCorrecta()))
                .forEach(respuesta -> eventPublisher.publishEvent(new RespuestaIncorrectaEvent(
                        respuesta.getId(), respuesta.getPregunta().getId(), usuarioId)));
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

    private Simulacro obtenerSimulacroDelUsuario(Long simulacroId) {
        Simulacro simulacro = simulacroRepository.findById(simulacroId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ese simulacro"));

        if (!simulacro.getUsuario().getId().equals(currentUserService.getUsuarioId())) {
            throw new ForbiddenException("Este simulacro no te pertenece");
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

    // El diagnostico gratuito se cobra con el contador del simulacro completo
    // mensual: no hay Funcionalidad.DIAGNOSTICO (un enum persistido exige migracion).
    private Funcionalidad funcionalidadSegunTipo(TipoSimulacro tipo) {
        return switch (tipo) {
            case POR_TEMA -> Funcionalidad.SIMULACRO_TEMA;
            case COMPLETO, DIAGNOSTICO -> Funcionalidad.SIMULACRO_COMPLETO;
        };
    }
}
