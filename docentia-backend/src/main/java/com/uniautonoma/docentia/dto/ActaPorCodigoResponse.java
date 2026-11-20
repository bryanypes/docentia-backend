package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.EstadoActa;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que la administración ve de un acta firmada (en el registro o al buscarla por su código): de
 * quién es, quién firmó y si está vigente. Sin nota, categoría ni compromisos.
 */
public record ActaPorCodigoResponse(
    Long idActa,
    String codigo,
    String nombreActa,
    EstadoActa estado,
    String periodo,
    String nombreDocente,
    String correoDocente,
    String programa,
    String escuela,
    LocalDateTime fechaFirma,
    List<Firmante> firmas,
    String hashContenido) {

  /** Quién firmó, con qué cargo y cuándo. */
  public record Firmante(String nombre, String cargo, String correo, LocalDateTime fecha) {}
}
