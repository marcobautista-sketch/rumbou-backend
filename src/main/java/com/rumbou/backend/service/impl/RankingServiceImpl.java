package com.rumbou.backend.service.impl;

import com.rumbou.backend.dto.request.ParticipacionRankingRequest;
import com.rumbou.backend.dto.response.RankingEntradaResponse;
import com.rumbou.backend.dto.response.RankingPaginaResponse;
import com.rumbou.backend.dto.response.RankingResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Universidad;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.exception.ForbiddenException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.ObjetivoUsuarioRepository;
import com.rumbou.backend.repository.RankingRepository;
import com.rumbou.backend.repository.UsuarioRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.RankingService;
import com.rumbou.backend.service.SemanaActividad;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class RankingServiceImpl implements RankingService {

    private final UsuarioRepository usuarioRepository;
    private final ObjetivoUsuarioRepository objetivoUsuarioRepository;
    private final RankingRepository rankingRepository;
    private final CurrentUserService currentUserService;
    private final Clock reloj;

    public RankingServiceImpl(UsuarioRepository usuarioRepository,
                              ObjetivoUsuarioRepository objetivoUsuarioRepository,
                              RankingRepository rankingRepository, CurrentUserService currentUserService,
                              Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.objetivoUsuarioRepository = objetivoUsuarioRepository;
        this.rankingRepository = rankingRepository;
        this.currentUserService = currentUserService;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public RankingResponse obtenerRanking(int page, int size) {
        return construirRanking(obtenerUsuario(), page, size);
    }

    @Override
    @Transactional
    public RankingResponse participar(ParticipacionRankingRequest request) {
        Usuario usuario = obtenerUsuario();
        if (usuario.getRole() != Role.USER) {
            throw new ForbiddenException("Solo los postulantes pueden participar en la liga");
        }
        String alias = request.alias();
        usuarioRepository.reservarAliasLiga(alias);
        if (usuarioRepository.existeAliasLigaDeOtroUsuario(alias, usuario.getId())) {
            throw aliasOcupado();
        }
        usuario.setAliasLiga(alias);
        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw aliasOcupado();
        }
        return construirRanking(usuario, 0, 10);
    }

    @Override
    @Transactional
    public void dejarDeParticipar() {
        Usuario usuario = obtenerUsuario();
        if (usuario.getAliasLiga() != null) {
            usuario.setAliasLiga(null);
            usuarioRepository.saveAndFlush(usuario);
        }
    }

    private Usuario obtenerUsuario() {
        return usuarioRepository.findById(currentUserService.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private DuplicateResourceException aliasOcupado() {
        return new DuplicateResourceException("Ese alias ya esta en uso; elige otro para la liga");
    }

    private RankingResponse construirRanking(Usuario usuario, int page, int size) {
        PageRequest paginacion = PageRequest.of(Math.max(0, page), size <= 0 ? 10 : Math.min(size, 50));
        LocalDate semanaInicio = SemanaActividad.inicio(SemanaActividad.hoy(reloj));
        Universidad universidad = objetivoUsuarioRepository
                .findFirstByUsuarioIdAndActivoTrueOrderByIdAsc(usuario.getId())
                .map(objetivo -> objetivo.getOfertaAcademica().getUniversidad()).orElse(null);
        Page<RankingRepository.Participante> participantes = universidad == null ? Page.empty(paginacion)
                : rankingRepository.obtenerParticipantes(universidad.getId(), semanaInicio, paginacion);
        Long posicion = universidad == null ? null
                : rankingRepository.obtenerPosicion(universidad.getId(), semanaInicio, usuario.getId());
        List<RankingEntradaResponse> entradas = IntStream.range(0, participantes.getNumberOfElements())
                .mapToObj(indice -> {
                    RankingRepository.Participante participante = participantes.getContent().get(indice);
                    return new RankingEntradaResponse(paginacion.getOffset() + indice + 1,
                            participante.getAlias(), participante.getXpSemanal(),
                            participante.getId().equals(usuario.getId()));
                }).toList();
        return new RankingResponse(universidad == null ? null : universidad.getSiglas(), semanaInicio,
                usuario.getAliasLiga() != null, usuario.getAliasLiga(), posicion,
                SemanaActividad.xpVigente(usuario, semanaInicio),
                new RankingPaginaResponse(entradas, participantes.getTotalElements(), participantes.getTotalPages(),
                        participantes.getNumber(), participantes.getSize()));
    }
}
