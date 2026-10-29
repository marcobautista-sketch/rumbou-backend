package com.rumbou.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbou.backend.client.gemini.GeminiClient;
import com.rumbou.backend.client.gemini.RespuestaAsistenteDto;
import com.rumbou.backend.dto.request.MensajePrevioRequest;
import com.rumbou.backend.dto.response.AreaResponse;
import com.rumbou.backend.dto.response.AsistenteResponse;
import com.rumbou.backend.dto.response.DominioTemaResponse;
import com.rumbou.backend.dto.response.GamificacionResponse;
import com.rumbou.backend.dto.response.ObjetivoResponse;
import com.rumbou.backend.entity.Funcionalidad;
import com.rumbou.backend.entity.RolMensaje;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.exception.PlanLimitExceededException;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.AreaService;
import com.rumbou.backend.service.AsistenteService;
import com.rumbou.backend.service.GamificacionService;
import com.rumbou.backend.service.ObjetivoService;
import com.rumbou.backend.service.PlanService;
import com.rumbou.backend.service.ProgresoService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AsistenteServiceImpl implements AsistenteService {

    private static final int MENSAJES_PREVIOS = 6;
    private static final int LARGO_MAXIMO_MENSAJE = 1000;

    // Formatos que acepta Gemini, con los nombres que usan Android, iOS y los navegadores para el mismo audio.
    private static final Map<String, String> TIPOS_AUDIO = Map.of(
            "audio/wav", "audio/wav", "audio/x-wav", "audio/wav", "audio/wave", "audio/wav",
            "audio/mp4", "audio/mp4", "audio/m4a", "audio/mp4", "audio/x-m4a", "audio/mp4",
            "audio/aac", "audio/aac", "audio/mpeg", "audio/mp3", "audio/webm", "audio/webm",
            "audio/ogg", "audio/ogg");

    private final GeminiClient geminiClient;
    private final PlanService planService;
    private final CurrentUserService currentUserService;
    private final ObjetivoService objetivoService;
    private final AreaService areaService;
    private final ProgresoService progresoService;
    private final GamificacionService gamificacionService;
    private final ObjectMapper objectMapper;

    public AsistenteServiceImpl(GeminiClient geminiClient, PlanService planService,
                                CurrentUserService currentUserService, ObjetivoService objetivoService,
                                AreaService areaService, ProgresoService progresoService,
                                GamificacionService gamificacionService, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.planService = planService;
        this.currentUserService = currentUserService;
        this.objetivoService = objetivoService;
        this.areaService = areaService;
        this.progresoService = progresoService;
        this.gamificacionService = gamificacionService;
        this.objectMapper = objectMapper;
    }

    // Cuenta como una consulta al tutor de IA: mismo limite de plan y mismo cupo diario.
    @Override
    public AsistenteResponse responderTexto(String mensaje, List<MensajePrevioRequest> historial) {
        Usuario usuario = currentUserService.getUsuario();
        planService.puedeAcceder(usuario, Funcionalidad.TUTOR_IA);

        String pregunta = mensaje.trim();
        RespuestaAsistenteDto respuesta = geminiClient.asistir(
                datosDelPostulante(usuario), conversacion(historial), pregunta, null, null);

        planService.registrarUso(usuario, Funcionalidad.TUTOR_IA);
        return new AsistenteResponse(pregunta, respuesta.respuesta());
    }

    @Override
    public AsistenteResponse responderVoz(MultipartFile audio, String historialJson) {
        String tipo = validarAudio(audio);
        List<MensajePrevioRequest> historial = leerHistorial(historialJson);
        Usuario usuario = currentUserService.getUsuario();
        planService.puedeAcceder(usuario, Funcionalidad.TUTOR_IA);

        RespuestaAsistenteDto respuesta = geminiClient.asistir(
                datosDelPostulante(usuario), conversacion(historial), null, leerBytes(audio), tipo);

        planService.registrarUso(usuario, Funcionalidad.TUTOR_IA);
        return new AsistenteResponse(respuesta.transcripcion(), respuesta.respuesta());
    }

    // Lo que el asistente sabe del postulante: sale de los mismos servicios que usan el panel y el perfil.
    private String datosDelPostulante(Usuario usuario) {
        StringBuilder datos = new StringBuilder();
        datos.append("Nombre: ").append(usuario.getNombre()).append('\n');
        datos.append("Fecha de hoy: ").append(LocalDate.now()).append('\n');

        GamificacionResponse gamificacion = gamificacionService.obtenerResumen();
        datos.append("Racha actual: ").append(gamificacion.rachaActual()).append(" dias; XP de esta semana: ")
                .append(gamificacion.xpSemanal()).append('\n');

        Map<Long, AreaResponse> areas = areaService.listar().stream()
                .collect(Collectors.toMap(AreaResponse::id, Function.identity()));
        List<ObjetivoResponse> objetivos = objetivoService.listarObjetivos();
        if (objetivos.isEmpty()) {
            datos.append("Aun no elige una carrera objetivo.\n");
        }
        objetivos.forEach(objetivo -> datos.append(describirObjetivo(objetivo, areas.get(objetivo.areaId())))
                .append('\n'));

        agregarTemas(datos);
        return datos.toString();
    }

    private String describirObjetivo(ObjetivoResponse objetivo, AreaResponse area) {
        StringBuilder texto = new StringBuilder("Objetivo: ").append(objetivo.carrera())
                .append(" en la ").append(objetivo.universidad())
                .append("; puntaje del ultimo ingresante: ").append(redondear(objetivo.puntajeUltimoIngresante()));
        if (objetivo.ultimoPsp() == null) {
            texto.append("; aun no rinde un simulacro completo para esta carrera");
        } else {
            texto.append("; su ultimo puntaje simulado (PSP): ").append(redondear(objetivo.ultimoPsp()))
                    .append("; indice de preparacion: ").append(redondear(objetivo.indicePreparacion()))
                    .append(" (").append(objetivo.estadoDescripcion()).append(')');
        }
        if (area != null && area.fechaProximoExamen() != null) {
            texto.append("; proximo examen: ").append(area.procesoProximo())
                    .append(" el ").append(area.fechaProximoExamen())
                    .append(Boolean.TRUE.equals(area.fechaExamenConfirmada()) ? " (fecha confirmada)" : " (fecha estimada)");
        }
        return texto.toString();
    }

    // El dominio por tema ya viene ordenado del mas debil al mas fuerte.
    private void agregarTemas(StringBuilder datos) {
        List<DominioTemaResponse> practicados;
        try {
            practicados = progresoService.dominioPorTema().stream().filter(tema -> tema.total() > 0).toList();
        } catch (PlanLimitExceededException ex) {
            return;
        }
        if (practicados.isEmpty()) {
            datos.append("Aun no responde preguntas en simulacros.\n");
            return;
        }
        datos.append("Temas mas debiles: ").append(describirTemas(practicados.subList(0, Math.min(3, practicados.size()))))
                .append('\n');
        datos.append("Temas mas fuertes: ")
                .append(describirTemas(practicados.reversed().subList(0, Math.min(2, practicados.size()))))
                .append('\n');
    }

    private static String describirTemas(List<DominioTemaResponse> temas) {
        return temas.stream()
                .map(tema -> "%s (%s%% de aciertos en %d preguntas)".formatted(
                        tema.tema(), redondear(tema.porcentajeAciertos()), tema.total()))
                .collect(Collectors.joining(", "));
    }

    private static String conversacion(List<MensajePrevioRequest> historial) {
        return historial.stream()
                .skip(Math.max(0, historial.size() - MENSAJES_PREVIOS))
                .map(mensaje -> (mensaje.rol() == RolMensaje.POSTULANTE ? "Postulante: " : "Asistente: ")
                        + mensaje.texto())
                .collect(Collectors.joining("\n"));
    }

    // En la voz el historial llega como texto JSON dentro del multipart: se valida a mano.
    private List<MensajePrevioRequest> leerHistorial(String historialJson) {
        if (historialJson == null || historialJson.isBlank()) {
            return List.of();
        }
        List<MensajePrevioRequest> mensajes;
        try {
            mensajes = objectMapper.readValue(historialJson, new TypeReference<List<MensajePrevioRequest>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new InvalidOperationException("El historial de la conversacion no tiene un formato valido");
        }
        return mensajes.stream()
                .filter(mensaje -> mensaje.rol() != null && mensaje.texto() != null && !mensaje.texto().isBlank())
                .map(mensaje -> new MensajePrevioRequest(mensaje.rol(), recortar(mensaje.texto())))
                .toList();
    }

    private String validarAudio(MultipartFile audio) {
        if (audio.isEmpty()) {
            throw new InvalidOperationException("El audio esta vacio");
        }
        String tipo = audio.getContentType() == null ? "" : audio.getContentType().toLowerCase(Locale.ROOT);
        // "audio/webm;codecs=opus" -> "audio/webm"
        String tipoBase = tipo.split(";")[0].trim();
        String tipoGemini = TIPOS_AUDIO.get(tipoBase);
        if (tipoGemini == null) {
            throw new InvalidOperationException("El audio debe ser WAV, M4A, AAC, MP3, OGG o WEBM");
        }
        return tipoGemini;
    }

    private byte[] leerBytes(MultipartFile audio) {
        try {
            return audio.getBytes();
        } catch (IOException ex) {
            throw new InvalidOperationException("No se pudo leer el audio; intenta grabarlo de nuevo");
        }
    }

    private static String recortar(String texto) {
        return texto.length() > LARGO_MAXIMO_MENSAJE ? texto.substring(0, LARGO_MAXIMO_MENSAJE) : texto;
    }

    private static String redondear(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor).replaceAll("\\.?0+$", "");
    }
}
