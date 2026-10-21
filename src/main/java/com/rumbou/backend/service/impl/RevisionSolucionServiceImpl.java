package com.rumbou.backend.service.impl;

import com.rumbou.backend.client.gemini.GeminiClient;
import com.rumbou.backend.client.gemini.RevisionSolucionDto;
import com.rumbou.backend.dto.response.RevisionSolucionResponse;
import com.rumbou.backend.entity.Funcionalidad;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.PlanService;
import com.rumbou.backend.service.RevisionSolucionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
public class RevisionSolucionServiceImpl implements RevisionSolucionService {

    // Los formatos de imagen que Gemini acepta; HEIC es el de la camara del iPhone.
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");

    private final PreguntaRepository preguntaRepository;
    private final GeminiClient geminiClient;
    private final PlanService planService;
    private final CurrentUserService currentUserService;

    public RevisionSolucionServiceImpl(PreguntaRepository preguntaRepository, GeminiClient geminiClient,
                                       PlanService planService, CurrentUserService currentUserService) {
        this.preguntaRepository = preguntaRepository;
        this.geminiClient = geminiClient;
        this.planService = planService;
        this.currentUserService = currentUserService;
    }

    // Cuenta como una consulta al tutor de IA: mismo limite de plan y mismo cupo diario.
    // Sin @Transactional: no se tiene una conexion abierta mientras Gemini mira la foto.
    @Override
    public RevisionSolucionResponse revisar(Long preguntaId, MultipartFile imagen) {
        String tipo = validarImagen(imagen);
        Pregunta pregunta = obtenerVisibleParaElUsuario(preguntaId);
        Usuario usuario = currentUserService.getUsuario();
        planService.puedeAcceder(usuario, Funcionalidad.TUTOR_IA);

        RevisionSolucionDto revision = geminiClient.revisarSolucion(pregunta.getEnunciado(),
                pregunta.getAlternativas(), pregunta.getClaveCorrecta(), pregunta.getExplicacion(),
                leerBytes(imagen), tipo);

        planService.registrarUso(usuario, Funcionalidad.TUTOR_IA);
        return new RevisionSolucionResponse(pregunta.getId(), revision.veredicto(), revision.retroalimentacion());
    }

    private String validarImagen(MultipartFile imagen) {
        if (imagen.isEmpty()) {
            throw new InvalidOperationException("La imagen esta vacia");
        }
        String tipo = imagen.getContentType() == null ? "" : imagen.getContentType().toLowerCase();
        if (!TIPOS_PERMITIDOS.contains(tipo)) {
            throw new InvalidOperationException("La imagen debe ser JPG, PNG, WEBP o HEIC");
        }
        return tipo;
    }

    private byte[] leerBytes(MultipartFile imagen) {
        try {
            return imagen.getBytes();
        } catch (IOException ex) {
            throw new InvalidOperationException("No se pudo leer la imagen; intenta subirla de nuevo");
        }
    }

    // Igual que en el banco de preguntas: para un postulante, una pregunta sin aprobar no existe.
    private Pregunta obtenerVisibleParaElUsuario(Long id) {
        Pregunta pregunta = preguntaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una pregunta con id " + id));
        if (!pregunta.isAprobada() && !currentUserService.puedeRevisarPreguntas()) {
            throw new ResourceNotFoundException("No existe una pregunta con id " + id);
        }
        return pregunta;
    }
}
