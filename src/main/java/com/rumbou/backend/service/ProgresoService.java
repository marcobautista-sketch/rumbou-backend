package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.DominioTemaResponse;
import com.rumbou.backend.dto.response.EstadoPreparacion;
import com.rumbou.backend.dto.response.HistorialPspResponse;
import com.rumbou.backend.dto.response.ObjetivoResponse;
import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.ObjetivoUsuario;
import com.rumbou.backend.entity.OfertaAcademica;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.exception.UnauthorizedException;
import com.rumbou.backend.repository.ObjetivoUsuarioRepository;
import com.rumbou.backend.repository.OfertaAcademicaRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.SimulacroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Progreso del postulante frente a sus carreras objetivo. Lo que depende del plan
// (cuantos objetivos puede tener, funciones PRO) se le pregunta a PlanService.
@Service
public class ProgresoService {

    private final ObjetivoUsuarioRepository objetivoUsuarioRepository;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final SimulacroRepository simulacroRepository;
    private final RespuestaUsuarioRepository respuestaUsuarioRepository;
    private final PlanService planService;

    public ProgresoService(ObjetivoUsuarioRepository objetivoUsuarioRepository,
                           OfertaAcademicaRepository ofertaAcademicaRepository,
                           SimulacroRepository simulacroRepository,
                           RespuestaUsuarioRepository respuestaUsuarioRepository,
                           PlanService planService) {
        this.objetivoUsuarioRepository = objetivoUsuarioRepository;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.simulacroRepository = simulacroRepository;
        this.respuestaUsuarioRepository = respuestaUsuarioRepository;
        this.planService = planService;
    }

    // IP = PSP / puntaje del ultimo ingresante. Al dividir por el corte de esa carrera
    // en esa universidad, las escalas distintas (1800 y 2000) se cancelan.
    public double calcularIp(double psp, double puntajeUltimoIngresante) {
        if (puntajeUltimoIngresante <= 0) {
            throw new IllegalArgumentException("El puntaje del ultimo ingresante debe ser positivo");
        }
        return psp / puntajeUltimoIngresante;
    }

    // Elegir una oferta que ya tuvo desactivada reutiliza la misma fila (la tabla
    // no permite repetir usuario + oferta).
    @Transactional
    public ObjetivoResponse crearObjetivo(Usuario usuario, Long ofertaAcademicaId) {
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaAcademicaId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la oferta academica indicada"));

        Optional<ObjetivoUsuario> existente = objetivoUsuarioRepository
                .findByUsuarioIdAndOfertaAcademicaId(usuario.getId(), ofertaAcademicaId);
        if (existente.isPresent() && existente.get().isActivo()) {
            throw new DuplicateResourceException("Esa carrera ya es uno de tus objetivos activos");
        }

        verificarLimiteDeObjetivos(usuario);

        ObjetivoUsuario objetivo = existente.orElseGet(
                () -> new ObjetivoUsuario(usuario, oferta, LocalDateTime.now()));
        objetivo.setActivo(true);
        inicializarConUltimoSimulacro(objetivo, usuario.getId(), oferta);
        objetivoUsuarioRepository.save(objetivo);

        return aResponse(objetivo);
    }

    @Transactional(readOnly = true)
    public List<ObjetivoResponse> listarObjetivos(Usuario usuario) {
        return objetivoUsuarioRepository.findByUsuarioIdAndActivoTrue(usuario.getId()).stream()
                .map(this::aResponse)
                .toList();
    }

    @Transactional
    public void desactivarObjetivo(Usuario usuario, Long objetivoId) {
        ObjetivoUsuario objetivo = objetivoUsuarioRepository.findById(objetivoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ese objetivo"));

        if (!objetivo.getUsuario().getId().equals(usuario.getId())) {
            throw new UnauthorizedException("Este objetivo no te pertenece");
        }

        objetivo.setActivo(false);
        objetivoUsuarioRepository.save(objetivo);
    }

    // Lo llama el listener de SimulacroFinalizadoEvent dentro de la transaccion que
    // finaliza el simulacro: si el usuario no tiene objetivos en esa area, no hace nada.
    // Una excepcion aqui revertiria la finalizacion del simulacro.
    @Transactional
    public void actualizarTrasSimulacro(Long usuarioId, Long areaId, double psp) {
        List<ObjetivoUsuario> objetivos = objetivoUsuarioRepository
                .findByUsuarioIdAndActivoTrueAndOfertaAcademicaAreaId(usuarioId, areaId);

        LocalDateTime ahora = LocalDateTime.now();
        for (ObjetivoUsuario objetivo : objetivos) {
            objetivo.setUltimoPsp(psp);
            objetivo.setUltimoIp(calcularIp(psp, objetivo.getOfertaAcademica().getPuntajeUltimoIngresante()));
            objetivo.setFechaActualizacion(ahora);
        }
        objetivoUsuarioRepository.saveAll(objetivos);
    }

    // PRO: ordenado del tema mas debil al mas fuerte, para mostrar primero las zonas de refuerzo.
    @Transactional(readOnly = true)
    public List<DominioTemaResponse> dominioPorTema(Usuario usuario) {
        verificarAccesoPro(usuario, "El dominio por tema es exclusivo del plan PRO");

        return respuestaUsuarioRepository.contarRespuestasPorTema(usuario.getId(), EstadoSimulacro.FINALIZADO)
                .stream()
                .map(this::aDominio)
                .sorted(Comparator.comparingDouble(DominioTemaResponse::porcentajeAciertos))
                .toList();
    }

    // PRO: la evolucion del PSP del usuario en un area, del simulacro mas antiguo al mas reciente.
    @Transactional(readOnly = true)
    public List<HistorialPspResponse> historialPsp(Usuario usuario, Long areaId) {
        verificarAccesoPro(usuario, "El historico de PSP es exclusivo del plan PRO");

        return simulacroRepository
                .findByUsuarioIdAndAreaIdAndEstadoOrderByFechaFinAsc(usuario.getId(), areaId, EstadoSimulacro.FINALIZADO)
                .stream()
                .map(simulacro -> new HistorialPspResponse(
                        simulacro.getId(),
                        simulacro.getTipo(),
                        simulacro.getFechaFin(),
                        simulacro.getPuntajeObtenido(),
                        simulacro.getPsp()))
                .toList();
    }

    // Un objetivo nuevo arranca con el PSP del ultimo simulacro que el usuario ya haya
    // finalizado en esa area, en vez de quedar vacio hasta el proximo simulacro.
    private void inicializarConUltimoSimulacro(ObjetivoUsuario objetivo, Long usuarioId, OfertaAcademica oferta) {
        simulacroRepository
                .findFirstByUsuarioIdAndAreaIdAndEstadoOrderByFechaFinDesc(
                        usuarioId, oferta.getArea().getId(), EstadoSimulacro.FINALIZADO)
                .filter(simulacro -> simulacro.getPsp() != null)
                .ifPresent(simulacro -> {
                    objetivo.setUltimoPsp(simulacro.getPsp());
                    objetivo.setUltimoIp(calcularIp(simulacro.getPsp(), oferta.getPuntajeUltimoIngresante()));
                    objetivo.setFechaActualizacion(simulacro.getFechaFin());
                });
    }

    private void verificarLimiteDeObjetivos(Usuario usuario) {
        long activos = objetivoUsuarioRepository.countByUsuarioIdAndActivoTrue(usuario.getId());
        int limite = planService.limiteObjetivosActivos(usuario);
        if (activos < limite) {
            return;
        }
        if (planService.tieneAccesoPro(usuario)) {
            throw new UnauthorizedException("Tu plan permite hasta " + limite + " objetivos activos a la vez");
        }
        throw new UnauthorizedException("El plan gratuito permite " + limite
                + " objetivo activo: desactiva el actual o pasate a PRO para tener hasta 3");
    }

    private void verificarAccesoPro(Usuario usuario, String mensaje) {
        if (!planService.tieneAccesoPro(usuario)) {
            throw new UnauthorizedException(mensaje);
        }
    }

    private DominioTemaResponse aDominio(RespuestaUsuarioRepository.ConteoPorTema conteo) {
        long correctas = conteo.getCorrectas();
        long incorrectas = conteo.getIncorrectas();
        long enBlanco = conteo.getEnBlanco();
        long total = correctas + incorrectas + enBlanco;
        double porcentaje = total == 0 ? 0 : Math.round(correctas * 1000.0 / total) / 10.0;

        return new DominioTemaResponse(conteo.getTemaId(), conteo.getTema(),
                correctas, incorrectas, enBlanco, total, porcentaje);
    }

    private ObjetivoResponse aResponse(ObjetivoUsuario objetivo) {
        OfertaAcademica oferta = objetivo.getOfertaAcademica();
        Double ip = objetivo.getUltimoIp();
        EstadoPreparacion estado = ip == null ? null : EstadoPreparacion.desde(ip);

        return new ObjetivoResponse(
                objetivo.getId(),
                oferta.getId(),
                oferta.getUniversidad().getSiglas(),
                oferta.getArea().getCodigo(),
                oferta.getCarrera().getNombre(),
                oferta.getProcesoAdmision(),
                oferta.getPuntajeUltimoIngresante(),
                objetivo.getUltimoPsp(),
                ip,
                estado,
                estado == null ? null : estado.getDescripcion(),
                objetivo.getFechaActualizacion());
    }
}
