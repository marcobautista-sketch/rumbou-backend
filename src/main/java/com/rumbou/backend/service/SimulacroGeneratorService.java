package com.rumbou.backend.service;

import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.EstructuraExamen;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.RespuestaUsuario;
import com.rumbou.backend.entity.Simulacro;
import com.rumbou.backend.entity.TipoSimulacro;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.repository.EstructuraExamenRepository;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.SimulacroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

// Arma un simulacro leyendo EstructuraExamen para el area indicada.
// Deliberadamente no recibe ni pregunta la universidad: el area ya trae
// implicita toda la informacion necesaria (que temas, cuantas preguntas,
// bajo que esquema se califican).
@Service
public class SimulacroGeneratorService {

    private final EstructuraExamenRepository estructuraExamenRepository;
    private final PreguntaRepository preguntaRepository;
    private final SimulacroRepository simulacroRepository;
    private final RespuestaUsuarioRepository respuestaUsuarioRepository;

    public SimulacroGeneratorService(EstructuraExamenRepository estructuraExamenRepository,
                                      PreguntaRepository preguntaRepository,
                                      SimulacroRepository simulacroRepository,
                                      RespuestaUsuarioRepository respuestaUsuarioRepository) {
        this.estructuraExamenRepository = estructuraExamenRepository;
        this.preguntaRepository = preguntaRepository;
        this.simulacroRepository = simulacroRepository;
        this.respuestaUsuarioRepository = respuestaUsuarioRepository;
    }

    @Transactional
    public Simulacro generar(Usuario usuario, Area area, TipoSimulacro tipo) {
        Simulacro simulacro = new Simulacro(usuario, area, tipo, LocalDateTime.now());
        simulacroRepository.save(simulacro);

        List<EstructuraExamen> estructura = estructuraExamenRepository.findByAreaIdOrderByOrden(area.getId());
        int preguntasAgregadas = 0;

        for (EstructuraExamen fila : estructura) {
            List<Pregunta> disponibles = preguntaRepository.findByTemaIdAndAprobadaTrue(fila.getTema().getId());
            Collections.shuffle(disponibles);

            int cantidad = Math.min(fila.getCantidadPreguntas(), disponibles.size());

            for (int i = 0; i < cantidad; i++) {
                RespuestaUsuario respuesta = new RespuestaUsuario(simulacro, disponibles.get(i), null, LocalDateTime.now());
                respuestaUsuarioRepository.save(respuesta);
                preguntasAgregadas++;
            }
        }

        // Sin banco de preguntas aprobadas el simulacro saldria vacio y el
        // postulante no entenderia por que. Mejor un error explicito.
        if (preguntasAgregadas == 0) {
            throw new InvalidOperationException(
                    "No hay preguntas aprobadas para armar un simulacro de esta area todavia");
        }

        return simulacro;
    }
}
