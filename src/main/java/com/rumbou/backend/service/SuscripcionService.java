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
import com.rumbou.backend.mapper.SuscripcionMapper;
import com.rumbou.backend.repository.SuscripcionRepository;
import com.rumbou.backend.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final MercadoPagoService mercadoPagoService;
    private final PlanService planService;
    private final CurrentUserService currentUserService;

    public SuscripcionService(SuscripcionRepository suscripcionRepository,
                              MercadoPagoService mercadoPagoService,
                              PlanService planService,
                              CurrentUserService currentUserService) {
        this.suscripcionRepository = suscripcionRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.planService = planService;
        this.currentUserService = currentUserService;
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
    public SuscripcionResponse crear() {
        Usuario usuario = currentUserService.getUsuario();
        if (planService.esPro(usuario.getId())) {
            throw new DuplicateResourceException("Ya tienes una suscripcion PRO vigente");
        }

        String externalReference = "usuario-" + usuario.getId();
        MercadoPagoService.ResultadoPreaprobacion resultado =
                mercadoPagoService.crearPreaprobacion(externalReference, usuario.getEmail());

        Suscripcion nueva = new Suscripcion(usuario, Plan.PRO, EstadoSuscripcion.PENDIENTE);
        nueva.setExternalReference(externalReference);
        nueva.setMercadoPagoPreapprovalId(resultado.preapprovalId());
        suscripcionRepository.save(nueva);

        return SuscripcionMapper.toResponse(nueva, resultado.initPoint());
    }

    // La suscripcion mas reciente del usuario: asi el cliente sabe si su pago ya se confirmo.
    @Transactional(readOnly = true)
    public SuscripcionResponse obtenerActual() {
        return suscripcionRepository.findFirstByUsuarioIdOrderByIdDesc(currentUserService.getUsuarioId())
                .map(suscripcion -> SuscripcionMapper.toResponse(suscripcion, null))
                .orElseThrow(() -> new ResourceNotFoundException("Todavia no tienes ninguna suscripcion"));
    }
}
