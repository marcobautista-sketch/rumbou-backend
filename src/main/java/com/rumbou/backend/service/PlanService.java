package com.rumbou.backend.service;

import com.rumbou.backend.entity.EstadoSuscripcion;
import com.rumbou.backend.entity.Funcionalidad;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Suscripcion;
import com.rumbou.backend.entity.UsoDiario;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.PlanLimitExceededException;
import com.rumbou.backend.repository.SuscripcionRepository;
import com.rumbou.backend.repository.UsoDiarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

// Puerta unica de limites: ningun otro modulo consulta la suscripcion directamente.
@Service
public class PlanService {

    private static final int GRATUITO_SIMULACROS_TEMA_SEMANAL = 3;
    private static final int GRATUITO_SIMULACROS_COMPLETOS_MENSUAL = 1;
    private static final int PRO_CONSULTAS_TUTOR_IA_DIARIO = 30;
    private static final int GRATUITO_OBJETIVOS_ACTIVOS = 1;
    private static final int PRO_OBJETIVOS_ACTIVOS = 3;

    private final SuscripcionRepository suscripcionRepository;
    private final UsoDiarioRepository usoDiarioRepository;

    public PlanService(SuscripcionRepository suscripcionRepository, UsoDiarioRepository usoDiarioRepository) {
        this.suscripcionRepository = suscripcionRepository;
        this.usoDiarioRepository = usoDiarioRepository;
    }

    public boolean esPro(Long usuarioId) {
        Optional<Suscripcion> vigente = suscripcionRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaInicioDesc(usuarioId, EstadoSuscripcion.ACTIVA);
        return vigente
                .map(s -> s.getFechaFin() == null || !s.getFechaFin().isBefore(LocalDate.now()))
                .orElse(false);
    }

    // Para funciones exclusivas de PRO que no consumen un contador de UsoDiario
    // (por ejemplo, el panel completo de progreso). El ADMIN pasa, igual que en puedeAcceder.
    public boolean tieneAccesoPro(Usuario usuario) {
        return usuario.getRole() == Role.ADMIN || esPro(usuario.getId());
    }

    // Cuantos objetivos (carreras) puede tener activos a la vez: 1 en el plan
    // gratuito, 3 en PRO y sin tope para el ADMIN.
    public int limiteObjetivosActivos(Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN) {
            return Integer.MAX_VALUE;
        }
        return esPro(usuario.getId()) ? PRO_OBJETIVOS_ACTIVOS : GRATUITO_OBJETIVOS_ACTIVOS;
    }

    // El ADMIN no tiene limites de plan: debe poder probar todo (incluido el tutor
    // de IA) sin pasar por Mercado Pago. Su uso se sigue registrando.
    public void puedeAcceder(Usuario usuario, Funcionalidad funcionalidad) {
        if (usuario.getRole() == Role.ADMIN) {
            return;
        }
        boolean pro = esPro(usuario.getId());
        switch (funcionalidad) {
            case TUTOR_IA -> verificarTutorIa(usuario.getId(), pro);
            case SIMULACRO_TEMA -> {
                if (!pro) {
                    verificarSimulacrosTemaSemanales(usuario.getId());
                }
            }
            case SIMULACRO_COMPLETO -> {
                if (!pro) {
                    verificarSimulacrosCompletosMensuales(usuario.getId());
                }
            }
        }
    }

    private void verificarTutorIa(Long usuarioId, boolean pro) {
        if (!pro) {
            throw new PlanLimitExceededException("El tutor de IA es exclusivo del plan PRO");
        }
        int usadas = usoDiarioRepository.sumConsultasTutorIA(usuarioId, LocalDate.now());
        if (usadas >= PRO_CONSULTAS_TUTOR_IA_DIARIO) {
            throw new PlanLimitExceededException("Limite alcanzado: 30 consultas al tutor de IA al dia");
        }
    }

    private void verificarSimulacrosTemaSemanales(Long usuarioId) {
        int usados = usoDiarioRepository.sumSimulacrosTemaDesde(usuarioId, LocalDate.now().minusDays(6));
        if (usados >= GRATUITO_SIMULACROS_TEMA_SEMANAL) {
            throw new PlanLimitExceededException("Limite alcanzado: 3 simulacros de tema a la semana");
        }
    }

    private void verificarSimulacrosCompletosMensuales(Long usuarioId) {
        int usados = usoDiarioRepository.sumSimulacrosCompletosDesde(usuarioId, LocalDate.now().minusMonths(1));
        if (usados >= GRATUITO_SIMULACROS_COMPLETOS_MENSUAL) {
            throw new PlanLimitExceededException("Limite alcanzado: 1 simulacro completo al mes");
        }
    }

    @Transactional
    public void registrarUso(Usuario usuario, Funcionalidad funcionalidad) {
        LocalDate hoy = LocalDate.now();
        UsoDiario uso = usoDiarioRepository.findByUsuarioIdAndFecha(usuario.getId(), hoy)
                .orElseGet(() -> usoDiarioRepository.save(new UsoDiario(usuario, hoy)));
        switch (funcionalidad) {
            case SIMULACRO_TEMA -> uso.incrementarSimulacrosTema();
            case SIMULACRO_COMPLETO -> uso.incrementarSimulacrosCompletos();
            case TUTOR_IA -> uso.incrementarConsultasTutorIA();
        }
    }
}