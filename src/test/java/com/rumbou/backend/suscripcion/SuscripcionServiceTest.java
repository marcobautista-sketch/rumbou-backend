package com.rumbou.backend.suscripcion;

import com.rumbou.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// JUnit puro con Mockito, como AuthServiceTest: la logica solo depende del repo.
class SuscripcionServiceTest {

    private SuscripcionRepository suscripcionRepository;
    private SuscripcionService suscripcionService;

    @BeforeEach
    void setUp() {
        suscripcionRepository = mock(SuscripcionRepository.class);
        suscripcionService = new SuscripcionService(suscripcionRepository);
    }

    @Test
    void activaLaSuscripcionPendienteAlRecibirElPago() {
        Suscripcion pendiente = new Suscripcion(null, Plan.PRO, EstadoSuscripcion.PENDIENTE);
        pendiente.setMercadoPagoPreapprovalId("preapproval-abc");
        when(suscripcionRepository.findFirstByMercadoPagoPreapprovalId("preapproval-abc"))
                .thenReturn(Optional.of(pendiente));

        PagoAprobadoEvent evento = new PagoAprobadoEvent(123L, "preapproval-abc", "usuario-7");
        suscripcionService.activar(evento);

        assertThat(pendiente.getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(pendiente.getFechaInicio()).isEqualTo(LocalDate.now());
        assertThat(pendiente.getFechaFin()).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    void noTocaLaSuscripcionSiYaEstabaActiva() {
        Suscripcion activa = new Suscripcion(null, Plan.PRO, EstadoSuscripcion.ACTIVA);
        activa.setMercadoPagoPreapprovalId("preapproval-abc");
        when(suscripcionRepository.findFirstByMercadoPagoPreapprovalId("preapproval-abc"))
                .thenReturn(Optional.of(activa));

        PagoAprobadoEvent evento = new PagoAprobadoEvent(123L, "preapproval-abc", "usuario-7");
        suscripcionService.activar(evento);

        assertThat(activa.getFechaInicio()).isNull();
        assertThat(activa.getFechaFin()).isNull();
        verify(suscripcionRepository, never()).save(any());
    }

    @Test
    void fallaSiNoExisteLaPreaprobacion() {
        when(suscripcionRepository.findFirstByMercadoPagoPreapprovalId("no-existe"))
                .thenReturn(Optional.empty());

        PagoAprobadoEvent evento = new PagoAprobadoEvent(999L, "no-existe", "usuario-99");
        assertThatThrownBy(() -> suscripcionService.activar(evento))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}