package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface RankingRepository extends Repository<Usuario, Long> {

    // Lista, total y posicion comparten exactamente la misma poblacion.
    String POBLACION = """
             from usuarios u
             where u.role = 'USER' and u.alias_liga is not null
               and u.xp_semanal > 0 and u.last_activity_date >= :semanaInicio
               and exists (
                   select 1 from objetivos_usuario o
                   join ofertas_academicas oferta on oferta.id = o.oferta_academica_id
                   where o.usuario_id = u.id and o.activo = true
                     and oferta.universidad_id = :universidadId
                     and o.id = (select min(principal.id) from objetivos_usuario principal
                                 where principal.usuario_id = u.id and principal.activo = true)
               )
            """;

    interface Participante {
        Long getId();
        String getAlias();
        int getXpSemanal();
    }

    @Query(value = "select u.id as id, u.alias_liga as alias, u.xp_semanal as xpSemanal "
            + POBLACION + " order by u.xp_semanal desc, u.id asc",
            countQuery = "select count(*) " + POBLACION, nativeQuery = true)
    Page<Participante> obtenerParticipantes(@Param("universidadId") Long universidadId,
                                           @Param("semanaInicio") LocalDate semanaInicio, Pageable pageable);

    @Query(value = "select clasificacion.posicion from (select u.id, "
            + "row_number() over (order by u.xp_semanal desc, u.id asc) as posicion "
            + POBLACION + ") clasificacion where clasificacion.id = :usuarioId", nativeQuery = true)
    Long obtenerPosicion(@Param("universidadId") Long universidadId,
                         @Param("semanaInicio") LocalDate semanaInicio, @Param("usuarioId") Long usuarioId);
}
