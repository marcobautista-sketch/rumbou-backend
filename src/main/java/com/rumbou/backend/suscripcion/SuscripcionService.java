package com.rumbou.backend.suscripcion;

import com.rumbou.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;

    public SuscripcionService(SuscripcionRepository suscripcionRepository) {
        this.suscripcionRepository = suscripcionRepository;
    }

    @Transactional
    public void activar(PagoAprobadoEvent evento) {
        Suscripcion suscripcion = suscripcionRepository
                .findFirstByMercadoPagoPreapprovalId(evento.mercadoPagoPreapprovalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe suscripcion con preaprobacion " + evento.mercadoPagoPreapprovalId()));

        if (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA) {
            return;   // guardia: si ya esta activa, no se vuelve a tocar (pago duplicado)
        }

        LocalDate hoy = LocalDate.now();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaInicio(hoy);
        suscripcion.setFechaFin(hoy.plusMonths(1));
    }
}