package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.dto.CoincidenciaComentariosResponse;
import com.uniautonoma.docentia.dto.DocenteResponse;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.DocenteService;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Busca una palabra en los comentarios de estudiantes del periodo (CSV, formulario anónimo y
 * comentario general de los formularios) y dice cuántos de cada docente la contienen. Solo
 * devuelve conteos: el texto de los comentarios nunca sale de aquí.
 */
@Service
public class BusquedaComentariosService {

  static final int LARGO_MINIMO = 3;

  private static final String SIN_TILDES = "translate(lower(%s), 'áéíóúüñ', 'aeiouun')";

  private static final String CONTAR =
      """
      select id_docente, count(*) as coincidencias from (
          select id_docente, texto from comentarios_estudiantes
          where id_periodo = :periodo and id_docente in (:ids)
          union all
          select docente_id, comentario from respuestas_heteroevaluacion
          where periodo_id = :periodo and docente_id in (:ids) and comentario is not null
          union all
          select docente_id, comentario_general from evaluaciones_formulario
          where periodo_id = :periodo and docente_id in (:ids) and estado = 'ENVIADA'
            and comentario_general is not null
      ) todos (id_docente, texto)
      where %s like :patron escape '\\'
      group by id_docente
      """
          .formatted(SIN_TILDES.formatted("texto"));

  private final NamedParameterJdbcTemplate jdbc;
  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;
  private final PeriodoAcademicoRepository periodoRepository;

  public BusquedaComentariosService(
      NamedParameterJdbcTemplate jdbc,
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      PeriodoAcademicoRepository periodoRepository) {
    this.jdbc = jdbc;
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.periodoRepository = periodoRepository;
  }

  /**
   * Docentes del alcance cuyos comentarios contienen el término, sin distinguir mayúsculas ni
   * tildes. Con menos de {@value #LARGO_MINIMO} letras no busca y devuelve la lista vacía.
   */
  @Transactional(readOnly = true)
  public List<CoincidenciaComentariosResponse> buscar(
      Long periodoId, String termino, UsuarioSesion sesion) {
    String limpio = normalizar(termino);
    if (limpio.length() < LARGO_MINIMO) {
      return List.of();
    }
    PeriodoAcademico periodo =
        periodoId == null
            ? periodoService.exigirPeriodoActivo()
            : periodoRepository
                .findById(periodoId)
                .orElseThrow(
                    () -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
    List<Long> ids =
        docenteService.listarDocentes(null, sesion).stream()
            .map(DocenteResponse::idDocente)
            .toList();
    if (ids.isEmpty()) {
      return List.of();
    }
    MapSqlParameterSource parametros =
        new MapSqlParameterSource()
            .addValue("periodo", periodo.getIdPeriodo())
            .addValue("ids", ids)
            .addValue("patron", "%" + escapar(limpio) + "%");
    return jdbc.query(
        CONTAR,
        parametros,
        (fila, i) ->
            new CoincidenciaComentariosResponse(
                fila.getLong("id_docente"), fila.getLong("coincidencias")));
  }

  /** Minúsculas y sin tildes, igual que la columna en la consulta. */
  static String normalizar(String termino) {
    if (termino == null) {
      return "";
    }
    String sinMarcas =
        Normalizer.normalize(termino.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
    return sinMarcas.length() > 60 ? sinMarcas.substring(0, 60) : sinMarcas;
  }

  private static String escapar(String texto) {
    return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
