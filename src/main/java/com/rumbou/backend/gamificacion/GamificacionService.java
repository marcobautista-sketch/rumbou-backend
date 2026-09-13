package com.rumbou.backend.gamificacion;

import com.rumbou.backend.auth.Usuario;
import com.rumbou.backend.auth.UsuarioRepository;
import com.rumbou.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class GamificacionService {

    // XP que gana el usuario por cada simulacro terminado.
    private static final int XP_POR_SIMULACRO = 100;

    private final UsuarioRepository usuarioRepository;
    private final LogroRepository logroRepository;
    private final UsuarioLogroRepository usuarioLogroRepository;

    public GamificacionService(UsuarioRepository usuarioRepository,
                               LogroRepository logroRepository,
                               UsuarioLogroRepository usuarioLogroRepository) {
        this.usuarioRepository = usuarioRepository;
        this.logroRepository = logroRepository;
        this.usuarioLogroRepository = usuarioLogroRepository;
    }

    @Transactional
    public void procesarSimulacroFinalizado(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        actualizarRachaYxp(usuario);
        evaluarLogros(usuario);
    }

    private void actualizarRachaYxp(Usuario usuario) {
        LocalDate hoy = LocalDate.now();
        int nuevaRacha = calcularNuevaRacha(usuario.getCurrentStreak(), usuario.getLastActivityDate(), hoy);

        usuario.setCurrentStreak(nuevaRacha);
        usuario.setLastActivityDate(hoy);
        if (nuevaRacha > usuario.getLongestStreak()) {
            usuario.setLongestStreak(nuevaRacha);
        }
        usuario.setXpTotal(usuario.getXpTotal() + XP_POR_SIMULACRO);
        usuario.setXpSemanal(usuario.getXpSemanal() + XP_POR_SIMULACRO);
    }

    // Funcion pura (sin base de datos): el corazon de la racha.
    // Tu ultima actividad fue hoy -> no cambia. Ayer -> +1. Antes -> se reinicia.
    public int calcularNuevaRacha(int currentStreak, LocalDate lastActivityDate, LocalDate hoy) {
        if (lastActivityDate == null) {
            return 1;
        }
        if (lastActivityDate.equals(hoy)) {
            return currentStreak;
        }
        if (lastActivityDate.equals(hoy.minusDays(1))) {
            return currentStreak + 1;
        }
        return 1;
    }

    private void evaluarLogros(Usuario usuario) {
        evaluarLogrosPorCondicion(usuario, TipoLogro.PRIMER_SIMULACRO);
        evaluarLogrosPorCondicion(usuario, TipoLogro.RACHA_DIAS);
        evaluarLogrosPorCondicion(usuario, TipoLogro.XP_TOTAL);
    }

    private void evaluarLogrosPorCondicion(Usuario usuario, TipoLogro condicion) {
        for (Logro logro : logroRepository.findByCondicion(condicion)) {
            if (seCumple(usuario, logro)) {
                desbloquearSiAplica(usuario, logro);
            }
        }
    }

    private boolean seCumple(Usuario usuario, Logro logro) {
        return switch (logro.getCondicion()) {
            case PRIMER_SIMULACRO -> true;
            case RACHA_DIAS -> usuario.getCurrentStreak() >= logro.getValorRequerido();
            case XP_TOTAL -> usuario.getXpTotal() >= logro.getValorRequerido();
        };
    }

    private void desbloquearSiAplica(Usuario usuario, Logro logro) {
        if (usuarioLogroRepository.existsByUsuarioIdAndLogroId(usuario.getId(), logro.getId())) {
            return;
        }
        usuarioLogroRepository.save(new UsuarioLogro(usuario, logro, LocalDateTime.now()));
    }
}