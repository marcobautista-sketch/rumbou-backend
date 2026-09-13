package com.rumbou.backend.gamificacion;

import com.rumbou.backend.examen.SimulacroFinalizadoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GamificacionListener {

    private final GamificacionService gamificacionService;

    public GamificacionListener(GamificacionService gamificacionService) {
        this.gamificacionService = gamificacionService;
    }

    @EventListener
    public void onSimulacroFinalizado(SimulacroFinalizadoEvent event) {
        gamificacionService.procesarSimulacroFinalizado(event.usuarioId());
    }
}