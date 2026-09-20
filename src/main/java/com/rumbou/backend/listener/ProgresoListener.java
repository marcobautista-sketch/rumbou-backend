package com.rumbou.backend.listener;

import com.rumbou.backend.event.SimulacroFinalizadoEvent;
import com.rumbou.backend.service.ProgresoService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Sincrono, dentro de la misma transaccion que finaliza el simulacro: si esa
// transaccion se revierte, el PSP e IP de los objetivos tampoco se guardan.
@Component
public class ProgresoListener {

    private final ProgresoService progresoService;

    public ProgresoListener(ProgresoService progresoService) {
        this.progresoService = progresoService;
    }

    @EventListener
    public void onSimulacroFinalizado(SimulacroFinalizadoEvent event) {
        progresoService.actualizarTrasSimulacro(event.usuarioId(), event.areaId(), event.psp());
    }
}
