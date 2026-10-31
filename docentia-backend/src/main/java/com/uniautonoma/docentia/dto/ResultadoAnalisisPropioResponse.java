package com.uniautonoma.docentia.dto;

import java.util.List;

/**
 * Lo que el docente ve del análisis: los hallazgos incluidos de la revisión ya publicada. Sin citas
 * ni comentarios, para no exponer a quien los escribió.
 *
 * @param periodo periodo de la revisión, o nulo si no hay ninguna publicada
 */
public record ResultadoAnalisisPropioResponse(
    PeriodoResumenResponse periodo, List<HallazgoPublicado> hallazgos) {

  public record HallazgoPublicado(String tipo, String texto) {}
}
