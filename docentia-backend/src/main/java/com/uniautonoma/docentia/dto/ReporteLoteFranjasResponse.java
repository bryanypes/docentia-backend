package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.dto.LoteFranjasRequest.AccionLote;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Qué se hizo y qué se omitió en una acción en lote, con el motivo de cada omisión. */
public record ReporteLoteFranjasResponse(
    AccionLote accion, List<Elemento> aplicadas, List<Elemento> omitidas) {

  /**
   * @param indice posición en la lista enviada (desde 0)
   * @param idFranja franja creada o cerrada; nula si se omitió al abrir
   * @param motivo por qué se omitió; nulo si se aplicó
   */
  public record Elemento(
      int indice,
      Long idFranja,
      LocalDate fecha,
      LocalTime horaInicio,
      LocalTime horaFin,
      String motivo) {}
}
