package com.rumbou.backend.service;

import com.rumbou.backend.entity.EsquemaCalificacion;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.RespuestaUsuario;
import com.rumbou.backend.entity.Universidad;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// Motor de calificacion dirigido por datos. No hay NINGUN if por
// universidad ni por nombre de prueba aqui: toda la variacion (valor del
// acierto, de la penalidad, puntaje maximo) llega como parametro dentro
// de EsquemaCalificacion. Esto es lo que permite que UNI y UNMSM corran
// por el mismo codigo. Si alguna vez sientes la necesidad de escribir
// "if (universidad == UNI)" en este archivo, el modelo esta mal.
@Service
public class CalificadorService {

    // Metodo puro: sin efectos secundarios, facil de probar con @ParameterizedTest.
    public double calcularPuntajeBloque(int correctas, int incorrectas, EsquemaCalificacion esquema) {
        return (correctas * esquema.getValorAcierto()) - (incorrectas * esquema.getValorPenalidad());
    }

    // Puntaje Simulado Proyectado: escala el puntaje obtenido en un simulacro
    // parcial al maximo oficial de la universidad, para poder comparar
    // cualquier simulacro (completo o por tema) contra el examen real.
    public double calcularPsp(double puntajeObtenido, double puntajeMaximoDelSimulacro, Universidad universidad) {
        if (puntajeMaximoDelSimulacro == 0) {
            return 0;
        }
        return (puntajeObtenido / puntajeMaximoDelSimulacro) * universidad.getPuntajeMaximo();
    }

    // Califica una respuesta individual: en blanco no suma ni resta,
    // correcta suma el acierto del esquema, incorrecta resta la penalidad.
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

    // Orquesta la calificacion de todas las respuestas de un simulacro y
    // devuelve el puntaje total (suma de los puntajeBloque, que en la
    // practica es la suma de puntajeAportado de cada respuesta).
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
