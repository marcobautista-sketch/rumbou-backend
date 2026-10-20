package com.rumbou.backend.service.impl;

import com.rumbou.backend.dto.response.DesafioDiarioResponse;
import com.rumbou.backend.entity.DesafioDiario;
import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.event.DesafioCompletadoEvent;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.mapper.DesafioMapper;
import com.rumbou.backend.repository.DesafioDiarioRepository;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository.ConteoPorTema;
import com.rumbou.backend.repository.TemaRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.DesafioService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DesafioServiceImpl implements DesafioService {

    static final int XP_POR_ACIERTO = 50;
    static final int XP_POR_PARTICIPAR = 10;
    static final int META_SEMANAL = 5;
    static final int XP_BONO_SEMANAL = 100;

    private final DesafioDiarioRepository desafioDiarioRepository;
    private final PreguntaRepository preguntaRepository;
    private final TemaRepository temaRepository;
    private final RespuestaUsuarioRepository respuestaUsuarioRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    public DesafioServiceImpl(DesafioDiarioRepository desafioDiarioRepository,
                              PreguntaRepository preguntaRepository,
                              TemaRepository temaRepository,
                              RespuestaUsuarioRepository respuestaUsuarioRepository,
                              CurrentUserService currentUserService,
                              ApplicationEventPublisher eventPublisher) {
        this.desafioDiarioRepository = desafioDiarioRepository;
        this.preguntaRepository = preguntaRepository;
        this.temaRepository = temaRepository;
        this.respuestaUsuarioRepository = respuestaUsuarioRepository;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }

    // La primera consulta del dia elige y guarda la pregunta; las siguientes devuelven la misma.
    @Transactional
    @Override
    public DesafioDiarioResponse obtenerDeHoy() {
        Usuario usuario = currentUserService.getUsuario();
        LocalDate hoy = LocalDate.now();
        DesafioDiario desafio = desafioDiarioRepository.findByUsuarioIdAndFecha(usuario.getId(), hoy)
                .orElseGet(() -> desafioDiarioRepository.save(
                        new DesafioDiario(usuario, hoy, elegirPregunta(usuario.getId()))));
        return DesafioMapper.toResponse(desafio, desafiosDeLaSemana(usuario.getId(), hoy), XP_POR_ACIERTO, META_SEMANAL);
    }

    // Acertar da 50 XP y fallar 10: intentarlo tambien mantiene la racha. El quinto desafio
    // de la semana suma el bono semanal.
    @Transactional
    @Override
    public DesafioDiarioResponse responder(int alternativaMarcada) {
        Usuario usuario = currentUserService.getUsuario();
        LocalDate hoy = LocalDate.now();
        DesafioDiario desafio = desafioDiarioRepository.findByUsuarioIdAndFecha(usuario.getId(), hoy)
                .orElseThrow(() -> new ResourceNotFoundException("Primero abre el desafio de hoy"));
        if (desafio.estaRespondido()) {
            throw new DuplicateResourceException("Ya respondiste el desafio de hoy");
        }

        boolean correcta = alternativaMarcada == desafio.getPregunta().getClaveCorrecta();
        long desafiosDeLaSemana = desafiosDeLaSemana(usuario.getId(), hoy) + 1;
        int xp = (correcta ? XP_POR_ACIERTO : XP_POR_PARTICIPAR)
                + (desafiosDeLaSemana == META_SEMANAL ? XP_BONO_SEMANAL : 0);

        desafio.registrarRespuesta(alternativaMarcada, correcta, xp);
        desafioDiarioRepository.save(desafio);
        eventPublisher.publishEvent(new DesafioCompletadoEvent(usuario.getId(), xp));
        return DesafioMapper.toResponse(desafio, desafiosDeLaSemana, XP_POR_ACIERTO, META_SEMANAL);
    }

    private Pregunta elegirPregunta(Long usuarioId) {
        Set<Long> yaUsadas = new HashSet<>(desafioDiarioRepository.findPreguntaIdsByUsuarioId(usuarioId));
        for (Long temaId : temasPorPrioridad(usuarioId)) {
            List<Pregunta> candidatas = preguntaRepository.findByTemaIdAndAprobadaTrue(temaId).stream()
                    .filter(pregunta -> !yaUsadas.contains(pregunta.getId()))
                    .toList();
            if (!candidatas.isEmpty()) {
                return candidatas.get(ThreadLocalRandom.current().nextInt(candidatas.size()));
            }
        }
        throw new ResourceNotFoundException("No hay preguntas disponibles para el desafio de hoy");
    }

    // Primero los temas con menor porcentaje de aciertos; despues, en orden aleatorio, los no practicados.
    private List<Long> temasPorPrioridad(Long usuarioId) {
        List<Long> practicados = respuestaUsuarioRepository
                .contarRespuestasPorTema(usuarioId, EstadoSimulacro.FINALIZADO).stream()
                .sorted(Comparator.comparingDouble(DesafioServiceImpl::porcentajeAciertos))
                .map(ConteoPorTema::getTemaId)
                .toList();
        List<Long> sinPracticar = new ArrayList<>(temaRepository.findAll().stream()
                .map(Tema::getId)
                .filter(temaId -> !practicados.contains(temaId))
                .toList());
        Collections.shuffle(sinPracticar);

        List<Long> prioridad = new ArrayList<>(practicados);
        prioridad.addAll(sinPracticar);
        return prioridad;
    }

    private static double porcentajeAciertos(ConteoPorTema conteo) {
        long total = conteo.getCorrectas() + conteo.getIncorrectas() + conteo.getEnBlanco();
        return total == 0 ? 0 : (double) conteo.getCorrectas() / total;
    }

    private long desafiosDeLaSemana(Long usuarioId, LocalDate hoy) {
        return desafioDiarioRepository.countByUsuarioIdAndFechaBetweenAndFechaRespuestaIsNotNull(
                usuarioId, hoy.with(DayOfWeek.MONDAY), hoy.with(DayOfWeek.SUNDAY));
    }
}
