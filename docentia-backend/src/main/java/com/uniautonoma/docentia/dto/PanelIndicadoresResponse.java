package com.uniautonoma.docentia.dto;

import java.math.BigDecimal;
import java.util.List;

/** Panel I11: agregados por programa y total del alcance. No identifica a ningún docente. */
public record PanelIndicadoresResponse(
    PeriodoResumenResponse periodo, List<FilaPanel> programas, FilaPanel total) {

  /**
   * Agregado de un programa o del total.
   *
   * @param idPrograma nulo en el total
   * @param cobertura porcentaje de docentes con promedio completo
   * @param promedio nulo si nadie tiene promedio completo
   */
  public record FilaPanel(
      Long idPrograma,
      String programa,
      String facultad,
      long docentes,
      long evaluados,
      BigDecimal cobertura,
      BigDecimal promedio,
      List<ConteoCategoria> distribucion) {}

  /** Cantidad de evaluados en una categoría y su porcentaje sobre los evaluados. */
  public record ConteoCategoria(
      String codigo, String etiqueta, long cantidad, BigDecimal porcentaje) {}
}
