package com.rumbou.backend.examen;

import com.rumbou.backend.academico.Area;
import com.rumbou.backend.academico.AreaRepository;
import com.rumbou.backend.academico.EstructuraExamen;
import com.rumbou.backend.academico.EstructuraExamenRepository;
import com.rumbou.backend.academico.EsquemaCalificacion;
import com.rumbou.backend.auth.Usuario;
import com.rumbou.backend.examen.dto.IniciarSimulacroRequest;
import com.rumbou.backend.examen.dto.PreguntaSimulacroResponse;
import com.rumbou.backend.examen.dto.ResponderPreguntaRequest;
import com.rumbou.backend.examen.dto.ResultadoSimulacroResponse;
import com.rumbou.backend.examen.dto.SimulacroResponse;
import com.rumbou.backend.shared.exception.InvalidOperationException;
import com.rumbou.backend.shared.exception.ResourceNotFoundException;
import com.rumbou.backend.shared.exception.UnauthorizedException;
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

    public SimulacroService(SimulacroRepository simulacroRepository,
                             RespuestaUsuarioRepository respuestaUsuarioRepository,
                             AreaRepository areaRepository,
                             EstructuraExamenRepository estructuraExamenRepository,
                             SimulacroGeneratorService simulacroGeneratorService,
                             CalificadorService calificadorService,
                             ApplicationEventPublisher eventPublisher) {
        this.simulacroRepository = simulacroRepository;
        this.respuestaUsuarioRepository = respuestaUsuarioRepository;
        this.areaRepository = areaRepository;
        this.estructuraExamenRepository = estructuraExamenRepository;
        this.simulacroGeneratorService = simulacroGeneratorService;
        this.calificadorService = calificadorService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SimulacroResponse iniciar(Usuario usuario, IniciarSimulacroRequest request) {
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el area indicada"));

        Simulacro simulacro = simulacroGeneratorService.generar(usuario, area, request.tipo());

        return construirRespuesta(simulacro);
    }

    @Transactional
    public void responder(Usuario usuario, Long simulacroId, ResponderPreguntaRequest request) {
        Simulacro simulacro = obtenerSimulacroDelUsuario(usuario, simulacroId);
        verificarEnCurso(simulacro);

        RespuestaUsuario respuesta = respuestaUsuarioRepository
                .findBySimulacroIdAndPreguntaId(simulacroId, request.preguntaId())
                .orElseThrow(() -> new InvalidOperationException("Esa pregunta no pertenece a este simulacro"));

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

        Map<Long, EsquemaCalificacion> esquemaPorTemaId = estructura.stream()
                .collect(Collectors.toMap(fila -> fila.getTema().getId(), EstructuraExamen::getEsquema));

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
                simulacro.getId(), usuario.getId(), simulacro.getArea().getId(), puntajeTotal, psp));

        for (RespuestaUsuario respuesta : respuestas) {
            if (Boolean.FALSE.equals(respuesta.getEsCorrecta())) {
                eventPublisher.publishEvent(new RespuestaIncorrectaEvent(
                        respuesta.getId(), respuesta.getPregunta().getId(), usuario.getId()));
            }
        }

        return new ResultadoSimulacroResponse(simulacro.getId(), puntajeTotal, psp, simulacro.getEstado());
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
}
