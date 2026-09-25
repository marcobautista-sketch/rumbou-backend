package com.rumbou.backend.service.impl;

import com.rumbou.backend.dto.response.ObjetivoResponse;
import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.ObjetivoUsuario;
import com.rumbou.backend.entity.OfertaAcademica;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.exception.ForbiddenException;
import com.rumbou.backend.exception.PlanLimitExceededException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.mapper.ProgresoMapper;
import com.rumbou.backend.repository.ObjetivoUsuarioRepository;
import com.rumbou.backend.repository.OfertaAcademicaRepository;
import com.rumbou.backend.repository.SimulacroRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.ObjetivoService;
import com.rumbou.backend.service.PlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ObjetivoServiceImpl implements ObjetivoService {

    private final ObjetivoUsuarioRepository objetivoUsuarioRepository;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final SimulacroRepository simulacroRepository;
    private final PlanService planService;
    private final CurrentUserService currentUserService;

    public ObjetivoServiceImpl(ObjetivoUsuarioRepository objetivoUsuarioRepository,
                               OfertaAcademicaRepository ofertaAcademicaRepository,
                               SimulacroRepository simulacroRepository,
                               PlanService planService,
                               CurrentUserService currentUserService) {
        this.objetivoUsuarioRepository = objetivoUsuarioRepository;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.simulacroRepository = simulacroRepository;
        this.planService = planService;
        this.currentUserService = currentUserService;
    }

    // IP = PSP / puntaje del ultimo ingresante. Al dividir por el corte de esa carrera
    // en esa universidad, las escalas distintas (1800 y 2000) se cancelan.
    public double calcularIp(double psp, double puntajeUltimoIngresante) {
        if (puntajeUltimoIngresante <= 0) {
            throw new IllegalArgumentException("El puntaje del ultimo ingresante debe ser positivo");
        }
        return psp / puntajeUltimoIngresante;
    }

    // Elegir una oferta que ya tuvo desactivada reutiliza la misma fila (la tabla
    // no permite repetir usuario + oferta).
    @Transactional
    @Override
    public ObjetivoResponse crearObjetivo(Long ofertaAcademicaId) {
        Usuario usuario = currentUserService.getUsuario();
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaAcademicaId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la oferta academica indicada"));

        Optional<ObjetivoUsuario> existente = objetivoUsuarioRepository
                .findByUsuarioIdAndOfertaAcademicaId(usuario.getId(), ofertaAcademicaId);
        if (existente.isPresent() && existente.get().isActivo()) {
            throw new DuplicateResourceException("Esa carrera ya es uno de tus objetivos activos");
        }

        verificarLimiteDeObjetivos(usuario);

        ObjetivoUsuario objetivo = existente.orElseGet(
                () -> new ObjetivoUsuario(usuario, oferta, LocalDateTime.now()));
        objetivo.setActivo(true);
        inicializarConUltimoSimulacro(objetivo, usuario.getId(), oferta);
        objetivoUsuarioRepository.save(objetivo);

        return ProgresoMapper.toObjetivoResponse(objetivo);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ObjetivoResponse> listarObjetivos() {
        return objetivoUsuarioRepository.findByUsuarioIdAndActivoTrue(currentUserService.getUsuarioId()).stream()
                .map(ProgresoMapper::toObjetivoResponse)
                .toList();
    }

    @Transactional
    @Override
    public void desactivarObjetivo(Long objetivoId) {
        ObjetivoUsuario objetivo = objetivoUsuarioRepository.findById(objetivoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ese objetivo"));

        if (!objetivo.getUsuario().getId().equals(currentUserService.getUsuarioId())) {
            throw new ForbiddenException("Este objetivo no te pertenece");
        }

        objetivo.setActivo(false);
        objetivoUsuarioRepository.save(objetivo);
    }

    // Lo llama el listener de SimulacroFinalizadoEvent dentro de la transaccion que
    // finaliza el simulacro: si el usuario no tiene objetivos en esa area, no hace nada.
    // Una excepcion aqui revertiria la finalizacion del simulacro.
    @Transactional
    @Override
    public void actualizarTrasSimulacro(Long usuarioId, Long areaId, double psp) {
        List<ObjetivoUsuario> objetivos = objetivoUsuarioRepository
                .findByUsuarioIdAndActivoTrueAndOfertaAcademicaAreaId(usuarioId, areaId);

        LocalDateTime ahora = LocalDateTime.now();
        for (ObjetivoUsuario objetivo : objetivos) {
            objetivo.setUltimoPsp(psp);
            objetivo.setUltimoIp(calcularIp(psp, objetivo.getOfertaAcademica().getPuntajeUltimoIngresante()));
            objetivo.setFechaActualizacion(ahora);
        }
        objetivoUsuarioRepository.saveAll(objetivos);
    }

    // Un objetivo nuevo arranca con el PSP del ultimo simulacro que el usuario ya haya
    // finalizado en esa area, en vez de quedar vacio hasta el proximo simulacro.
    private void inicializarConUltimoSimulacro(ObjetivoUsuario objetivo, Long usuarioId, OfertaAcademica oferta) {
        simulacroRepository
                .findFirstByUsuarioIdAndAreaIdAndEstadoOrderByFechaFinDesc(
                        usuarioId, oferta.getArea().getId(), EstadoSimulacro.FINALIZADO)
                .filter(simulacro -> simulacro.getPsp() != null)
                .ifPresent(simulacro -> {
                    objetivo.setUltimoPsp(simulacro.getPsp());
                    objetivo.setUltimoIp(calcularIp(simulacro.getPsp(), oferta.getPuntajeUltimoIngresante()));
                    objetivo.setFechaActualizacion(simulacro.getFechaFin());
                });
    }

    private void verificarLimiteDeObjetivos(Usuario usuario) {
        long activos = objetivoUsuarioRepository.countByUsuarioIdAndActivoTrue(usuario.getId());
        int limite = planService.limiteObjetivosActivos(usuario);
        if (activos < limite) {
            return;
        }
        if (planService.tieneAccesoPro(usuario)) {
            throw new PlanLimitExceededException("Tu plan permite hasta " + limite + " objetivos activos a la vez");
        }
        throw new PlanLimitExceededException("El plan gratuito permite " + limite
                + " objetivo activo: desactiva el actual o pasate a PRO para tener hasta 3");
    }
}
