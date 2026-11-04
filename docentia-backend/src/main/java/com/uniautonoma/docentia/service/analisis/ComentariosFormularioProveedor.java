package com.uniautonoma.docentia.service.analisis;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Comentarios del formulario anónimo de heteroevaluación. No se copian al enviar: se leen aquí, al
 * analizar, y solo el texto. Nunca se lee el id de la respuesta, ni la asignación, ni nada que
 * permita ordenarlas por llegada; el orden es el de la huella SHA-256 del texto y el identificador
 * es la posición en ese orden.
 */
@Component
public class ComentariosFormularioProveedor implements ProveedorComentarios {

  private static final String CONTAR =
      "select count(*) from respuestas_heteroevaluacion where docente_id = ? and periodo_id = ?";

  private static final String COMENTARIOS =
      """
      select comentario from respuestas_heteroevaluacion
      where docente_id = ? and periodo_id = ? and comentario is not null and btrim(comentario) <> ''
      order by encode(sha256(convert_to(comentario, 'UTF8')), 'hex'), comentario
      """;

  private final JdbcTemplate jdbcTemplate;

  public ComentariosFormularioProveedor(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Respuestas enviadas, con o sin comentario: el mínimo se cuenta sobre respuestas. */
  @Override
  public int contarEncuestasCerradas(Long idDocente, Long idPeriodo) {
    Integer total = jdbcTemplate.queryForObject(CONTAR, Integer.class, idDocente, idPeriodo);
    return total == null ? 0 : total;
  }

  @Override
  public List<ComentarioFuente> comentarios(Long idDocente, Long idPeriodo) {
    List<String> textos =
        jdbcTemplate.queryForList(COMENTARIOS, String.class, idDocente, idPeriodo);
    List<ComentarioFuente> comentarios = new ArrayList<>(textos.size());
    for (int i = 0; i < textos.size(); i++) {
      comentarios.add(new ComentarioFuente((long) i + 1, textos.get(i)));
    }
    return comentarios;
  }
}
