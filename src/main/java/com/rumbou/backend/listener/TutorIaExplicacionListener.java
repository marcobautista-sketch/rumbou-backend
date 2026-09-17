package com.rumbou.backend.listener;

import com.rumbou.backend.client.gemini.GeminiClient;
import com.rumbou.backend.client.gemini.GeminiException;
import com.rumbou.backend.event.RespuestaIncorrectaEvent;
import com.rumbou.backend.repository.PreguntaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// @Async + AFTER_COMMIT: llama a Gemini y necesita la respuesta ya confirmada en BD.
// Es la explicacion estatica (una vez por pregunta, gratis para todos), no el tutor PRO.
@Component
public class TutorIaExplicacionListener {

    private static final Logger log = LoggerFactory.getLogger(TutorIaExplicacionListener.class);

    private final PreguntaRepository preguntaRepository;
    private final GeminiClient geminiClient;

    public TutorIaExplicacionListener(PreguntaRepository preguntaRepository, GeminiClient geminiClient) {
        this.preguntaRepository = preguntaRepository;
        this.geminiClient = geminiClient;
    }

    // REQUIRES_NEW: en AFTER_COMMIT la transaccion original ya se cerro; con @Transactional normal Spring no arranca.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
                log.warn("No se pudo generar explicacion para la pregunta {}: {}", pregunta.getId(), ex.getMessage());
            }
        });
    }
}
