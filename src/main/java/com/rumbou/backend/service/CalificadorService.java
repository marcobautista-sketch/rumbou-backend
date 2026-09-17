package com.rumbou.backend.service;

import com.rumbou.backend.entity.EsquemaCalificacion;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.RespuestaUsuario;
import com.rumbou.backend.entity.Universidad;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// Calificacion dirigida por datos: no hay ningun if por universidad ni por
// prueba; acierto, penalidad y maximo llegan dentro de EsquemaCalificacion.
@Service
public class CalificadorService {

    public double calcularPuntajeBloque(int correctas, int incorrectas, EsquemaCalificacion esquema) {
        return (correctas * esquema.getValorAcierto()) - (incorrectas * esquema.getValorPenalidad());
    }

    // PSP: escala el puntaje de un simulacro (completo o por tema) al maximo oficial de la universidad.
    public double calcularPsp(double puntajeObtenido, double puntajeMaximoDelSimulacro, Universidad universidad) {
        if (puntajeMaximoDelSimulacro == 0) {
            return 0;
        }
        return (puntajeObtenido / puntajeMaximoDelSimulacro) * universidad.getPuntajeMaximo();
    }

    // En blanco no suma ni resta; correcta suma el acierto; incorrecta resta la penalidad.
    public void calificarRespuesta(RespuestaUsuario respuesta, EsquemaCalificacion esquema) {
        Integer marcada = respuesta.getAlternativaMarcada();

        if (marcada == null) {
            respuesta.setEsCorrecta(null);
            respuesta.setPuntajeAportado(0.0);
            return;
        }

        Pregunta pregunta = respuesta.getPregunta();
        boolean esCorrecta = marcada == pregunta.getClaveCorrecta();
        respuesta.setEsCorrecta(esCorrecta);
        respuesta.setPuntajeAportado(esCorrecta ? esquema.getValorAcierto() : -esquema.getValorPenalidad());
    }

    public double calificarSimulacro(List<RespuestaUsuario> respuestas, Map<Long, EsquemaCalificacion> esquemaPorTemaId) {
        double puntajeTotal = 0;

        for (RespuestaUsuario respuesta : respuestas) {
            Long temaId = respuesta.getPregunta().getTema().getId();
            EsquemaCalificacion esquema = esquemaPorTemaId.get(temaId);
            calificarRespuesta(respuesta, esquema);
            puntajeTotal += respuesta.getPuntajeAportado();
        }

        return puntajeTotal;
    }
}
