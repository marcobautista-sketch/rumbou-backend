package com.rumbou.backend.service.impl;

import com.rumbou.backend.dto.response.DominioTemaResponse;
import com.rumbou.backend.dto.response.HistorialPspResponse;
import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.PlanLimitExceededException;
import com.rumbou.backend.mapper.ProgresoMapper;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.SimulacroRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.PlanService;
import com.rumbou.backend.service.ProgresoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ProgresoServiceImpl implements ProgresoService {

    private final SimulacroRepository simulacroRepository;
    private final RespuestaUsuarioRepository respuestaUsuarioRepository;
    private final PlanService planService;
    private final CurrentUserService currentUserService;

    public ProgresoServiceImpl(SimulacroRepository simulacroRepository,
                               RespuestaUsuarioRepository respuestaUsuarioRepository,
                               PlanService planService,
                               CurrentUserService currentUserService) {
        this.simulacroRepository = simulacroRepository;
        this.respuestaUsuarioRepository = respuestaUsuarioRepository;
        this.planService = planService;
        this.currentUserService = currentUserService;
    }

    // Ordenado del tema mas debil al mas fuerte, para mostrar primero las zonas de refuerzo.
    @Transactional(readOnly = true)
    @Override
    public List<DominioTemaResponse> dominioPorTema() {
        Usuario usuario = currentUserService.getUsuario();
        verificarAccesoPro(usuario, "El dominio por tema es exclusivo del plan PRO");

        return respuestaUsuarioRepository.contarRespuestasPorTema(usuario.getId(), EstadoSimulacro.FINALIZADO)
                .stream()
                .map(ProgresoMapper::toDominioResponse)
                .sorted(Comparator.comparingDouble(DominioTemaResponse::porcentajeAciertos))
                .toList();
    }

    // La evolucion del PSP del usuario en un area, del simulacro mas antiguo al mas reciente.
    @Transactional(readOnly = true)
    @Override
    public List<HistorialPspResponse> historialPsp(Long areaId) {
        Usuario usuario = currentUserService.getUsuario();
        verificarAccesoPro(usuario, "El historico de PSP es exclusivo del plan PRO");

        return simulacroRepository
                .findByUsuarioIdAndAreaIdAndEstadoOrderByFechaFinAsc(usuario.getId(), areaId, EstadoSimulacro.FINALIZADO)
                .stream()
                .map(ProgresoMapper::toHistorialResponse)
                .toList();
    }

    private void verificarAccesoPro(Usuario usuario, String mensaje) {
        if (!planService.tieneAccesoPro(usuario)) {
            throw new PlanLimitExceededException(mensaje);
        }
    }
}
