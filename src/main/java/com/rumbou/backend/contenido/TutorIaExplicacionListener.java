package com.rumbou.backend.contenido;

import com.rumbou.backend.contenido.gemini.GeminiClient;
import com.rumbou.backend.contenido.gemini.GeminiException;
import com.rumbou.backend.examen.RespuestaIncorrectaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// @Async porque llama a un servicio externo, AFTER_COMMIT porque necesita que la
// respuesta incorrecta ya este confirmada en base de datos.
@Component
public class TutorIaExplicacionListener {

    private static final Logger log = LoggerFactory.getLogger(TutorIaExplicacionListener.class);

    private final PreguntaRepository preguntaRepository;
    private final GeminiClient geminiClient;

    public TutorIaExplicacionListener(PreguntaRepository preguntaRepository, GeminiClient geminiClient) {
        this.preguntaRepository = preguntaRepository;
        this.geminiClient = geminiClient;
    }

    // REQUIRES_NEW es obligatorio aqui: en AFTER_COMMIT la transaccion original
    // ya se cerro, asi que para guardar la explicacion hay que abrir una nueva.
    // Con @Transactional normal, Spring ni siquiera arranca la aplicacion.
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
