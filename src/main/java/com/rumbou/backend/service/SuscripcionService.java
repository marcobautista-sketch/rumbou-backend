package com.rumbou.backend.service;

import com.rumbou.backend.client.mercadopago.MercadoPagoService;
import com.rumbou.backend.dto.response.SuscripcionResponse;
import com.rumbou.backend.entity.EstadoSuscripcion;
import com.rumbou.backend.entity.Plan;
import com.rumbou.backend.entity.Suscripcion;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.event.PagoAprobadoEvent;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.SuscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final MercadoPagoService mercadoPagoService;

    public SuscripcionService(SuscripcionRepository suscripcionRepository,
                              MercadoPagoService mercadoPagoService) {
        this.suscripcionRepository = suscripcionRepository;
        this.mercadoPagoService = mercadoPagoService;
    }

    @Transactional
    public void activar(PagoAprobadoEvent evento) {
        Suscripcion suscripcion = suscripcionRepository
                .findFirstByMercadoPagoPreapprovalId(evento.mercadoPagoPreapprovalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe suscripcion con preaprobacion " + evento.mercadoPagoPreapprovalId()));

        if (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA) {
            return;
        }

        LocalDate hoy = LocalDate.now();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaInicio(hoy);
        suscripcion.setFechaFin(hoy.plusMonths(1));
    }

    @Transactional
    public SuscripcionResponse crear(Usuario usuario) {
        Optional<Suscripcion> vigente = suscripcionRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaInicioDesc(usuario.getId(), EstadoSuscripcion.ACTIVA);
        if (vigente.isPresent() && (vigente.get().getFechaFin() == null
                || !vigente.get().getFechaFin().isBefore(LocalDate.now()))) {
            throw new DuplicateResourceException("Ya tienes una suscripcion PRO vigente");
        }

        String externalReference = "usuario-" + usuario.getId();
        MercadoPagoService.ResultadoPreaprobacion resultado =
                mercadoPagoService.crearPreaprobacion(externalReference, usuario.getEmail());

        Suscripcion nueva = new Suscripcion(usuario, Plan.PRO, EstadoSuscripcion.PENDIENTE);
        nueva.setExternalReference(externalReference);
        nueva.setMercadoPagoPreapprovalId(resultado.preapprovalId());
        suscripcionRepository.save(nueva);

        return new SuscripcionResponse(
                nueva.getId(), nueva.getPlan().name(), nueva.getEstado().name(),
                nueva.getFechaInicio(), nueva.getFechaFin(), resultado.initPoint());
    }
}