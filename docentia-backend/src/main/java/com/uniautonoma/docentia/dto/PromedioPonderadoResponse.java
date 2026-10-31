package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.service.ClasificadorDesempeno;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Resultado del promedio ponderado. Si faltan fuentes pero hay al menos una, es PARCIAL: los pesos
 * se renormalizan entre las disponibles. Sin ninguna fuente no hay nota, nunca un 0.
 *
 * @param categoria categoría de desempeño de la Resolución 074, nula si no hay nota
 */
public record PromedioPonderadoResponse(
    EstadoPromedio estado,
    BigDecimal valor,
    List<FuenteEvaluacion> fuentesPendientes,
    CategoriaResponse categoria) {

  public PromedioPonderadoResponse(
      EstadoPromedio estado, BigDecimal valor, List<FuenteEvaluacion> fuentesPendientes) {
    this(
        estado,
        valor,
        fuentesPendientes,
        valor == null ? null : CategoriaResponse.desde(ClasificadorDesempeno.clasificar(valor)));
  }

  public static PromedioPonderadoResponse completo(BigDecimal valor) {
    return new PromedioPonderadoResponse(EstadoPromedio.COMPLETO, valor, List.of());
  }

  public static PromedioPonderadoResponse parcial(
      BigDecimal valor, List<FuenteEvaluacion> fuentesPendientes) {
    return new PromedioPonderadoResponse(EstadoPromedio.PARCIAL, valor, fuentesPendientes);
  }

  public static PromedioPonderadoResponse sinNota() {
    return new PromedioPonderadoResponse(
        EstadoPromedio.SIN_NOTA, null, Arrays.asList(FuenteEvaluacion.values()));
  }

  /** Lo que ve el docente: un número parcial podría revelar la nota de una sola fuente (A4). */
  public PromedioPonderadoResponse sinValorSiEsParcial() {
    return estado == EstadoPromedio.PARCIAL
        ? new PromedioPonderadoResponse(estado, null, fuentesPendientes, null)
        : this;
  }
}
