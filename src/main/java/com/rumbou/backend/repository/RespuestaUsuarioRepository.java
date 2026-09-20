package com.rumbou.backend.repository;

import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.RespuestaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RespuestaUsuarioRepository extends JpaRepository<RespuestaUsuario, Long> {

    List<RespuestaUsuario> findBySimulacroId(Long simulacroId);

    Optional<RespuestaUsuario> findBySimulacroIdAndPreguntaId(Long simulacroId, Long preguntaId);

    // Progreso: dominio por tema. Al calificar, esCorrecta queda en null para las
    // preguntas en blanco, true para las correctas y false para las incorrectas.
    @Query("""
            SELECT t.id AS temaId, t.nombre AS tema,
                   SUM(CASE WHEN r.esCorrecta = true THEN 1 ELSE 0 END) AS correctas,
                   SUM(CASE WHEN r.esCorrecta = false THEN 1 ELSE 0 END) AS incorrectas,
                   SUM(CASE WHEN r.alternativaMarcada IS NULL THEN 1 ELSE 0 END) AS enBlanco
            FROM RespuestaUsuario r
            JOIN r.pregunta p
            JOIN p.tema t
            JOIN r.simulacro s
            WHERE s.usuario.id = :usuarioId AND s.estado = :estado
            GROUP BY t.id, t.nombre
            """)
    List<ConteoPorTema> contarRespuestasPorTema(@Param("usuarioId") Long usuarioId,
                                                @Param("estado") EstadoSimulacro estado);

    interface ConteoPorTema {
        Long getTemaId();

        String getTema();

        Long getCorrectas();

        Long getIncorrectas();

        Long getEnBlanco();
    }
}
