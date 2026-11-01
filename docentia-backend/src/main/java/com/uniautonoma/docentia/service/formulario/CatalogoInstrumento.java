package com.uniautonoma.docentia.service.formulario;

import com.uniautonoma.docentia.entity.TipoFormulario;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lee el instrumento del Anexo 1, que vive en la base y no cambia en tiempo de ejecución. */
@Component
public class CatalogoInstrumento {

  public static final String VERSION = "RES074-2026-ANEXO1";

  private static final String LEER =
      """
      select c.id as id_criterio, c.codigo as criterio, c.nombre,
             d.id as id_descriptor, d.codigo as descriptor, d.texto
      from descriptores_evaluacion d
      join criterios_evaluacion c on c.id = d.criterio_id
      join descriptor_actor a on a.descriptor_id = d.id
      where a.tipo = ? and d.instrumento_version = ?
      order by c.orden, d.orden
      """;

  private final JdbcTemplate jdbcTemplate;

  public CatalogoInstrumento(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public record Descriptor(Long id, String codigo, String texto) {}

  public record Criterio(Long id, String codigo, String nombre, List<Descriptor> descriptores) {}

  /** Criterios con los descriptores que responde un tipo de formulario, en orden. */
  public List<Criterio> de(TipoFormulario tipo) {
    Map<String, Criterio> criterios = new LinkedHashMap<>();
    jdbcTemplate.query(
        LEER,
        fila -> {
          criterios
              .computeIfAbsent(
                  fila.getString("criterio"),
                  codigo -> {
                    try {
                      return new Criterio(
                          fila.getLong("id_criterio"),
                          codigo,
                          fila.getString("nombre"),
                          new ArrayList<>());
                    } catch (java.sql.SQLException fallo) {
                      throw new IllegalStateException(fallo);
                    }
                  })
              .descriptores()
              .add(
                  new Descriptor(
                      fila.getLong("id_descriptor"),
                      fila.getString("descriptor"),
                      fila.getString("texto")));
        },
        tipo.name(),
        VERSION);
    return List.copyOf(criterios.values());
  }
}
