package com.uniautonoma.docentia.service.analisis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniautonoma.docentia.entity.FuenteComentarios;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Etiqueta;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Resultados estructurados del análisis y la copia anonimizada de lo analizado. Cada corrida es una
 * versión nueva; la anterior queda marcada como reemplazada y no se borra.
 */
@Repository
public class ResultadosAnalisisRepositorio {

  /** Un comentario de la copia: solo texto anonimizado, huella y etiquetas. */
  public record ComentarioGuardado(
      int indice,
      Long idCita,
      String hash,
      String texto,
      String sentimiento,
      List<Etiqueta> etiquetas) {}

  /** Resultado vigente o de una versión concreta. */
  public record Guardado(
      Long id,
      Long idSolicitud,
      FuenteComentarios fuente,
      int version,
      LocalDateTime generadoEn,
      ResultadoAnalisis resultado) {}

  /** Página de la copia de comentarios. */
  public record Pagina(List<ComentarioGuardado> comentarios, long total) {}

  private static final TypeReference<List<Etiqueta>> ETIQUETAS = new TypeReference<>() {};

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public ResultadosAnalisisRepositorio(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  /**
   * Guarda una versión nueva y reemplaza la vigente. Debe correr en una transacción.
   *
   * @return la versión asignada
   */
  public int guardar(
      Long idSolicitud,
      Long idDocente,
      Long idPeriodo,
      FuenteComentarios fuente,
      ResultadoAnalisis resultado,
      List<ComentarioGuardado> comentarios) {
    // El bloqueo del docente serializa dos corridas del mismo docente que terminen a la vez.
    jdbcTemplate.queryForList(
        "select id_usuario from usuarios where id_usuario = ? for update", Long.class, idDocente);
    jdbcTemplate.update(
        "update resultados_analisis_comentarios set reemplazado = true"
            + " where id_docente = ? and id_periodo = ? and not reemplazado",
        idDocente,
        idPeriodo);
    Integer anterior =
        jdbcTemplate.queryForObject(
            "select coalesce(max(version), 0) from resultados_analisis_comentarios"
                + " where id_docente = ? and id_periodo = ?",
            Integer.class,
            idDocente,
            idPeriodo);
    int version = (anterior == null ? 0 : anterior) + 1;
    Long id =
        jdbcTemplate.queryForObject(
            "insert into resultados_analisis_comentarios (id_solicitud, id_docente, id_periodo,"
                + " fuente, version, tipo_modelo, version_modelo, version_lexico, n_comentarios,"
                + " n_utiles, resultado) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id",
            Long.class,
            idSolicitud,
            idDocente,
            idPeriodo,
            fuente.name(),
            version,
            resultado.tipoModelo(),
            resultado.versionModelo(),
            resultado.versionLexicoOPrompt(),
            resultado.nComentarios()
                + resultado.nExcluidos().values().stream().mapToInt(Integer::intValue).sum(),
            resultado.nComentarios(),
            json(resultado));
    jdbcTemplate.batchUpdate(
        "insert into comentarios_analizados"
            + " (id_resultado, indice, id_cita, hash, texto, sentimiento, etiquetas)"
            + " values (?, ?, ?, ?, ?, ?, ?)",
        comentarios.stream()
            .map(
                c ->
                    new Object[] {
                      id,
                      c.indice(),
                      c.idCita(),
                      c.hash(),
                      c.texto(),
                      c.sentimiento(),
                      json(c.etiquetas())
                    })
            .toList());
    return version;
  }

  public Optional<Guardado> vigente(Long idDocente, Long idPeriodo) {
    return jdbcTemplate
        .query(
            "select id, id_solicitud, fuente, version, generado_en, resultado"
                + " from resultados_analisis_comentarios"
                + " where id_docente = ? and id_periodo = ? and not reemplazado",
            this::guardado,
            idDocente,
            idPeriodo)
        .stream()
        .findFirst();
  }

  public Optional<Guardado> porSolicitud(Long idSolicitud) {
    return jdbcTemplate
        .query(
            "select id, id_solicitud, fuente, version, generado_en, resultado"
                + " from resultados_analisis_comentarios where id_solicitud = ?",
            this::guardado,
            idSolicitud)
        .stream()
        .findFirst();
  }

  /** Resultado vigente del periodo anterior más reciente según sus fechas, para las tendencias. */
  public Optional<ResultadoAnalisis> anterior(Long idDocente, Long idPeriodo) {
    return jdbcTemplate
        .query(
            """
            select r.resultado from resultados_analisis_comentarios r
            join periodos_academicos p on p.id_periodo = r.id_periodo
            where r.id_docente = ? and not r.reemplazado
              and p.fecha_inicio < (select fecha_inicio from periodos_academicos where id_periodo = ?)
            order by p.fecha_inicio desc
            limit 1
            """,
            (fila, n) -> leer(fila.getString(1)),
            idDocente,
            idPeriodo)
        .stream()
        .findFirst();
  }

  /** Textos anonimizados por el identificador con que el modelo los citó. */
  public Map<Long, String> textosPorCita(Long idResultado) {
    Map<Long, String> textos = new HashMap<>();
    jdbcTemplate.query(
        "select id_cita, texto from comentarios_analizados where id_resultado = ?",
        fila -> {
          textos.put(fila.getLong(1), fila.getString(2));
        },
        idResultado);
    return textos;
  }

  /** Página de la copia, filtrada por criterio y sentimiento, en el orden de la huella. */
  public Pagina comentarios(
      Long idResultado, String criterio, String sentimiento, int pagina, int tamano) {
    StringBuilder donde = new StringBuilder(" where id_resultado = ?");
    List<Object> parametros = new ArrayList<>(List.of(idResultado));
    if (criterio != null) {
      donde.append(" and etiquetas like ?");
      parametros.add("%\"criterio\":\"" + criterio + "\"%");
    }
    if (sentimiento != null) {
      donde.append(" and sentimiento = ?");
      parametros.add(sentimiento);
    }
    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from comentarios_analizados" + donde,
            Long.class,
            parametros.toArray());
    List<Object> conPagina = new ArrayList<>(parametros);
    conPagina.add(tamano);
    conPagina.add((long) pagina * tamano);
    List<ComentarioGuardado> filas =
        jdbcTemplate.query(
            "select indice, id_cita, hash, texto, sentimiento, etiquetas from comentarios_analizados"
                + donde
                + " order by indice limit ? offset ?",
            (fila, n) ->
                new ComentarioGuardado(
                    fila.getInt(1),
                    fila.getLong(2),
                    fila.getString(3),
                    fila.getString(4),
                    fila.getString(5),
                    leerEtiquetas(fila.getString(6))),
            conPagina.toArray());
    return new Pagina(filas, total == null ? 0 : total);
  }

  /**
   * Resultados vigentes del periodo, de los docentes de un programa o una facultad (nulos: todos).
   */
  public List<ResultadoAnalisis> vigentesDelPeriodo(
      Long idPeriodo, Long idPrograma, Long idFacultad) {
    return jdbcTemplate.query(
        """
        select r.resultado from resultados_analisis_comentarios r
        join usuarios u on u.id_usuario = r.id_docente
        join programas pr on pr.id_programa = u.id_programa
        where r.id_periodo = ? and not r.reemplazado
          and (cast(? as bigint) is null or pr.id_programa = ?)
          and (cast(? as bigint) is null or pr.id_facultad = ?)
        order by r.id
        """,
        (fila, n) -> leer(fila.getString(1)),
        idPeriodo,
        idPrograma,
        idPrograma,
        idFacultad,
        idFacultad);
  }

  private Guardado guardado(ResultSet fila, int n) throws SQLException {
    return new Guardado(
        fila.getLong("id"),
        fila.getLong("id_solicitud"),
        FuenteComentarios.valueOf(fila.getString("fuente")),
        fila.getInt("version"),
        fila.getTimestamp("generado_en").toLocalDateTime(),
        leer(fila.getString("resultado")));
  }

  private ResultadoAnalisis leer(String json) {
    try {
      return objectMapper.readValue(json, ResultadoAnalisis.class);
    } catch (JsonProcessingException fallo) {
      throw new IllegalStateException("Resultado de análisis ilegible", fallo);
    }
  }

  private List<Etiqueta> leerEtiquetas(String json) {
    try {
      return objectMapper.readValue(json, ETIQUETAS);
    } catch (JsonProcessingException fallo) {
      throw new IllegalStateException("Etiquetas ilegibles", fallo);
    }
  }

  private String json(Object valor) {
    try {
      return objectMapper.writeValueAsString(valor);
    } catch (JsonProcessingException fallo) {
      throw new IllegalStateException("No se pudo guardar el resultado", fallo);
    }
  }
}
