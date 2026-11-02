package com.uniautonoma.docentia.service.heteroevaluacion;

import com.uniautonoma.docentia.dto.CoberturaHeteroevaluacionResponse;
import com.uniautonoma.docentia.dto.CoberturaHeteroevaluacionResponse.Fila;
import com.uniautonoma.docentia.dto.PaginaResponse;
import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cobertura de la heteroevaluación: cuántos matriculados activos respondieron. */
@Service
@Transactional(readOnly = true)
public class CoberturaService {

  public static final int TAMANO_MAXIMO = 50;

  /** Por docente: un estudiante cuenta por cada asignación en la que está matriculado. */
  private static final String POR_DOCENTE =
      """
      select u.id_usuario as id, u.nombre_completo as nombre, pr.nombre as programa,
             count(m.id) as matriculados, count(pa.estudiante_id) as respondieron
      from clases_docente cd
      join clases c on c.id = cd.clase_id and c.id_periodo = ?
      join matriculas m on m.clase_id = c.id and m.es_activa
      left join participaciones_heteroevaluacion pa
             on pa.asignacion_id = cd.id and pa.estudiante_id = m.estudiante_id
      join usuarios u on u.id_usuario = cd.docente_id
      join programas pr on pr.id_programa = u.id_programa
      where (? = 0 or pr.id_facultad = ?)
      group by u.id_usuario, u.nombre_completo, pr.nombre
      """;

  private final JdbcTemplate jdbcTemplate;
  private final PeriodoAcademicoService periodoService;
  private final PeriodoAcademicoRepository periodoRepository;

  public CoberturaService(
      JdbcTemplate jdbcTemplate,
      PeriodoAcademicoService periodoService,
      PeriodoAcademicoRepository periodoRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.periodoService = periodoService;
    this.periodoRepository = periodoRepository;
  }

  public CoberturaHeteroevaluacionResponse calcular(
      Long idPeriodo, UsuarioSesion sesion, int pagina, int tamano) {
    PeriodoAcademico periodo =
        idPeriodo == null
            ? periodoService.exigirPeriodoActivo()
            : periodoRepository
                .findById(idPeriodo)
                .orElseThrow(
                    () -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
    // 0 significa toda la institución.
    long idFacultad = sesion.esCoordinador() ? sesion.idFacultad() : 0L;
    Object[] filtro = {periodo.getIdPeriodo(), idFacultad, idFacultad};

    List<Fila> programas =
        jdbcTemplate.query(
            "select programa, sum(matriculados) as matriculados, sum(respondieron) as respondieron"
                + " from ("
                + POR_DOCENTE
                + ") d group by programa order by programa",
            (rs, n) ->
                fila(
                    null,
                    rs.getString("programa"),
                    rs.getString("programa"),
                    rs.getLong("matriculados"),
                    rs.getLong("respondieron")),
            filtro);
    long matriculados = programas.stream().mapToLong(Fila::matriculados).sum();
    long respondieron = programas.stream().mapToLong(Fila::respondieron).sum();

    int tamanoReal = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
    int paginaReal = Math.max(pagina, 0);
    long totalDocentes =
        jdbcTemplate.queryForObject(
            "select count(*) from (" + POR_DOCENTE + ") d", Long.class, filtro);
    List<Object> conPagina = new ArrayList<>(List.of(filtro));
    conPagina.add(tamanoReal);
    conPagina.add((long) paginaReal * tamanoReal);
    List<Fila> docentes =
        jdbcTemplate.query(
            POR_DOCENTE + " order by u.nombre_completo limit ? offset ?",
            (rs, n) ->
                fila(
                    rs.getLong("id"),
                    rs.getString("nombre"),
                    rs.getString("programa"),
                    rs.getLong("matriculados"),
                    rs.getLong("respondieron")),
            conPagina.toArray());
    return new CoberturaHeteroevaluacionResponse(
        PeriodoResumenResponse.desde(periodo),
        fila(null, "Total", null, matriculados, respondieron),
        programas,
        new PaginaResponse<>(
            docentes,
            paginaReal,
            tamanoReal,
            totalDocentes,
            (int) Math.ceil((double) totalDocentes / tamanoReal)));
  }

  private static Fila fila(
      Long id, String nombre, String programa, long matriculados, long respondieron) {
    return new Fila(
        id,
        nombre,
        programa,
        matriculados,
        respondieron,
        matriculados == 0
            ? BigDecimal.ZERO.setScale(1)
            : BigDecimal.valueOf(respondieron * 100)
                .divide(BigDecimal.valueOf(matriculados), 1, RoundingMode.HALF_UP));
  }

  /** Cobertura de una facultad en el periodo, para el aviso de cobertura baja. */
  public BigDecimal porcentajeFacultad(Long idPeriodo, Long idFacultad) {
    List<long[]> filas =
        jdbcTemplate.query(
            "select sum(matriculados) as m, sum(respondieron) as r from (" + POR_DOCENTE + ") d",
            (rs, n) -> new long[] {rs.getLong("m"), rs.getLong("r")},
            idPeriodo,
            idFacultad,
            idFacultad);
    long[] total = filas.isEmpty() ? new long[] {0, 0} : filas.get(0);
    return fila(null, "", null, total[0], total[1]).porcentaje();
  }

  public boolean tieneMatriculados(Long idPeriodo, Long idFacultad) {
    return jdbcTemplate.queryForObject(
            "select count(*) from (" + POR_DOCENTE + ") d",
            Long.class,
            idPeriodo,
            idFacultad,
            idFacultad)
        > 0;
  }
}
