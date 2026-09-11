package com.rumbou.backend.contenido;

import com.rumbou.backend.contenido.gemini.GeminiClient;
import com.rumbou.backend.contenido.gemini.GeminiException;
import com.rumbou.backend.examen.RespuestaIncorrectaEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Tutor de IA: cuando examen/SimulacroService publica RespuestaIncorrectaEvent,
// generamos y cacheamos una explicacion en Pregunta.explicacion si todavia no tiene una.
// @Async porque llama a un servicio externo (Gemini), AFTER_COMMIT porque necesita que
// la respuesta incorrecta ya este confirmada en base de datos (ver tabla de eventos
// del CLAUDE.md).
@Component
public class TutorIaExplicacionListener {

    private final PreguntaRepository preguntaRepository;
    private final GeminiClient geminiClient;

    public TutorIaExplicacionListener(PreguntaRepository preguntaRepository, GeminiClient geminiClient) {
        this.preguntaRepository = preguntaRepository;
        this.geminiClient = geminiClient;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void alRecibirRespuestaIncorrecta(RespuestaIncorrectaEvent event) {
        preguntaRepository.findById(event.preguntaId()).ifPresent(pregunta -> {
            if (pregunta.getExplicacion() != null && !pregunta.getExplicacion().isBlank()) {
                return;
            }

            try {
                String explicacion = geminiClient.explicar(
                        pregunta.getEnunciado(), pregunta.getAlternativas(), pregunta.getClaveCorrecta());
                pregunta.setExplicacion(explicacion);
                preguntaRepository.save(pregunta);
            } catch (GeminiException ex) {
                // Es un enriquecimiento opcional: si Gemini falla, la pregunta se queda
                // sin explicacion cacheada por ahora, pero no debe tumbar nada mas.
                System.out.println("No se pudo generar explicacion para la pregunta "
                        + pregunta.getId() + ": " + ex.getMessage());
            }
        });
    }
}
