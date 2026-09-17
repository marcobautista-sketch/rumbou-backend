package com.rumbou.backend.service;

import com.rumbou.backend.entity.Logro;
import com.rumbou.backend.entity.TipoLogro;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.entity.UsuarioLogro;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.LogroRepository;
import com.rumbou.backend.repository.UsuarioLogroRepository;
import com.rumbou.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class GamificacionService {

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

    // Racha: hoy no cambia, ayer +1, antes se reinicia.
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