package com.rumbou.backend.mapper;

import com.rumbou.backend.dto.response.DominioTemaResponse;
import com.rumbou.backend.dto.response.EstadoPreparacion;
import com.rumbou.backend.dto.response.HistorialPspResponse;
import com.rumbou.backend.dto.response.ObjetivoResponse;
import com.rumbou.backend.entity.ObjetivoUsuario;
import com.rumbou.backend.entity.OfertaAcademica;
import com.rumbou.backend.entity.Simulacro;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;

public final class ProgresoMapper {

    private ProgresoMapper() {
    }

    // El semaforo no se guarda: se deduce del IP al armar la respuesta.
    public static ObjetivoResponse toObjetivoResponse(ObjetivoUsuario objetivo) {
        OfertaAcademica oferta = objetivo.getOfertaAcademica();
        Double ip = objetivo.getUltimoIp();
        EstadoPreparacion estado = ip == null ? null : EstadoPreparacion.desde(ip);

        return new ObjetivoResponse(
                objetivo.getId(),
                oferta.getId(),
                oferta.getUniversidad().getSiglas(),
                oferta.getArea().getId(),
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

    public static DominioTemaResponse toDominioResponse(RespuestaUsuarioRepository.ConteoPorTema conteo) {
        long correctas = conteo.getCorrectas();
        long incorrectas = conteo.getIncorrectas();
        long enBlanco = conteo.getEnBlanco();
        long total = correctas + incorrectas + enBlanco;
        double porcentaje = total == 0 ? 0 : Math.round(correctas * 1000.0 / total) / 10.0;

        return new DominioTemaResponse(conteo.getTemaId(), conteo.getTema(),
                correctas, incorrectas, enBlanco, total, porcentaje);
    }

    public static HistorialPspResponse toHistorialResponse(Simulacro simulacro) {
        return new HistorialPspResponse(
                simulacro.getId(),
                simulacro.getTipo(),
                simulacro.getFechaFin(),
                simulacro.getPuntajeObtenido(),
                simulacro.getPsp());
    }
}
