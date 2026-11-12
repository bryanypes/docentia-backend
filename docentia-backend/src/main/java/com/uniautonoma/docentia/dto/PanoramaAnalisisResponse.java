package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Panorama del análisis de comentarios en el alcance: solo conteos, nunca textos de comentarios ni
 * de hallazgos.
 *
 * @param docentes uno por docente del alcance, en orden por nombre
 */
public record PanoramaAnalisisResponse(
    PeriodoResumenResponse periodo, Totales totales, List<DocenteAnalisis> docentes) {

  /**
   * @param conComentarios docentes con al menos un comentario de estudiantes en el periodo
   * @param analizados docentes con un análisis completado
   * @param publicados docentes con la revisión publicada (el docente ya puede verla en su reunión)
   * @param pendientesRevision análisis completados que coordinación aún no publica
   */
  public record Totales(
      int docentes,
      int conComentarios,
      long comentarios,
      int analizados,
      int publicados,
      int pendientesRevision,
      int enCurso,
      int conError,
      int insuficientes,
      long comentariosEnviados,
      long comentariosFiltrados,
      int fortalezas,
      int oportunidades,
      int descartados,
      int porDecidir,
      int citas,
      int citasVerificadas) {}

  /**
   * @param estado del último análisis del periodo, o nulo si nunca se pidió
   * @param fortalezas hallazgos de fortaleza que no se descartaron
   * @param porDecidir hallazgos que coordinación aún no incluye ni descarta
   */
  public record DocenteAnalisis(
      DocenteResponse docente,
      long comentarios,
      Long idSolicitud,
      EstadoSolicitudAnalisis estado,
      boolean publicado,
      Integer comentariosEnviados,
      Integer comentariosFiltrados,
      int fortalezas,
      int oportunidades,
      int descartados,
      int porDecidir,
      int citas,
      int citasVerificadas,
      LocalDateTime fecha) {}
}
