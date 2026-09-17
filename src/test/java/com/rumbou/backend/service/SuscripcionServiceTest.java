package com.rumbou.backend.service;

import com.rumbou.backend.client.mercadopago.MercadoPagoService;
import com.rumbou.backend.dto.response.SuscripcionResponse;
import com.rumbou.backend.entity.EstadoSuscripcion;
import com.rumbou.backend.entity.Plan;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Suscripcion;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.event.PagoAprobadoEvent;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.SuscripcionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuscripcionServiceTest {

    private SuscripcionRepository suscripcionRepository;
    private SuscripcionService suscripcionService;
    private MercadoPagoService mercadoPagoService;
    private Usuario usuarioConId(long id) {
        Usuario usuario = new Usuario("postulante" + id + "@rumbou.com", "hash", "Ana", Role.USER);
        usuario.setId(id);
        return usuario;
    }

    @BeforeEach
    void setUp() {
        suscripcionRepository = mock(SuscripcionRepository.class);
        mercadoPagoService = mock(MercadoPagoService.class);
        suscripcionService = new SuscripcionService(suscripcionRepository, mercadoPagoService);
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

    @Test
    void creaSuscripcionPendienteConLinkDePago() {
        Usuario usuario = usuarioConId(7);
        when(mercadoPagoService.crearPreaprobacion("usuario-7", usuario.getEmail()))
                .thenReturn(new MercadoPagoService.ResultadoPreaprobacion(
                        "preaprobacion-abc", "usuario-7", "https://init.point/pago"));
        when(suscripcionRepository.findFirstByUsuarioIdAndEstadoOrderByFechaInicioDesc(
                anyLong(), any(EstadoSuscripcion.class))).thenReturn(Optional.empty());

        SuscripcionResponse respuesta = suscripcionService.crear(usuario);

        assertThat(respuesta.plan()).isEqualTo("PRO");
        assertThat(respuesta.estado()).isEqualTo("PENDIENTE");
        assertThat(respuesta.linkPago()).isEqualTo("https://init.point/pago");
        assertThat(respuesta.id()).isNull(); // aun no lo devuelve JPA hasta el flush
    }
}