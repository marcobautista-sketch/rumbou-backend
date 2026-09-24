package com.rumbou.backend.listener;

import com.rumbou.backend.entity.TipoSimulacro;
import com.rumbou.backend.event.SimulacroFinalizadoEvent;
import com.rumbou.backend.service.ObjetivoService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Sincrono, dentro de la misma transaccion que finaliza el simulacro: si esa
// transaccion se revierte, el PSP e IP de los objetivos tampoco se guardan.
@Component
public class ProgresoListener {

    private final ObjetivoService objetivoService;

    public ProgresoListener(ObjetivoService objetivoService) {
        this.objetivoService = objetivoService;
    }

    // Un simulacro POR_TEMA no mueve el IP: su PSP sale de un solo tema y no
    // representa el examen completo (10 de 10 en Trigonometria daria IP > 1).
    @EventListener
    public void onSimulacroFinalizado(SimulacroFinalizadoEvent event) {
        if (event.tipo() == TipoSimulacro.POR_TEMA) {
            return;
        }
        objetivoService.actualizarTrasSimulacro(event.usuarioId(), event.areaId(), event.psp());
    }
}
