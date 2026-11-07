package com.rumbou.backend.repository;

import com.rumbou.backend.dto.request.ParticipacionRankingRequest;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.impl.RankingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankingConcurrenciaTest extends AbstractContainerBaseTest {

    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private ObjetivoUsuarioRepository objetivos;
    @Autowired
    private RankingRepository ranking;
    @Autowired
    private PlatformTransactionManager transacciones;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void soloUnaInscripcionSimultaneaPuedeReservarElMismoAliasSinDistinguirMayusculas() throws Exception {
        TransactionTemplate transaccion = new TransactionTemplate(transacciones);
        Long primero = transaccion.execute(estado -> usuarios.saveAndFlush(
                new Usuario("carrera1@example.com", "hash", "Privado", Role.USER)).getId());
        Long segundo = transaccion.execute(estado -> usuarios.saveAndFlush(
                new Usuario("carrera2@example.com", "hash", "Privado", Role.USER)).getId());
        CountDownLatch preparados = new CountDownLatch(2);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var uno = ejecutor.submit(inscribir(primero, "Ñandú", preparados));
            var dos = ejecutor.submit(inscribir(segundo, "ñANDÚ", preparados));

            assertThat(uno.get(20, TimeUnit.SECONDS) + dos.get(20, TimeUnit.SECONDS)).isEqualTo(1);
            transaccion.executeWithoutResult(estado -> {
                long inscritos = usuarios.findAllById(java.util.List.of(primero, segundo)).stream()
                        .filter(usuario -> usuario.getAliasLiga() != null).count();
                assertThat(inscritos).isEqualTo(1);
            });
        } finally {
            transaccion.executeWithoutResult(estado -> usuarios.deleteAllById(java.util.List.of(primero, segundo)));
        }
    }

    private Callable<Integer> inscribir(Long usuarioId, String alias, CountDownLatch preparados) {
        return () -> {
            CurrentUserService actual = mock(CurrentUserService.class);
            when(actual.getUsuarioId()).thenReturn(usuarioId);
            RankingServiceImpl servicio = new RankingServiceImpl(usuarios, objetivos, ranking, actual, Clock.systemUTC());
            preparados.countDown();
            assertThat(preparados.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                return new TransactionTemplate(transacciones).execute(estado -> {
                    servicio.participar(new ParticipacionRankingRequest(alias));
                    return 1;
                });
            } catch (DuplicateResourceException ex) {
                return 0;
            }
        };
    }
}
