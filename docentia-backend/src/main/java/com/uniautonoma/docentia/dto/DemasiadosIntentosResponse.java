package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.exception.DemasiadosIntentosException;
import java.util.Map;

/** Cuerpo del 429: el error de siempre más los segundos que faltan, para mostrar un contador. */
public record DemasiadosIntentosResponse(
    String mensaje, Map<String, String> errores, Long segundosParaReintentar) {

  public static DemasiadosIntentosResponse desde(DemasiadosIntentosException excepcion) {
    long segundos = excepcion.getSegundosParaReintentar();
    return new DemasiadosIntentosResponse(
        excepcion.getMessage(), Map.of(), segundos > 0 ? segundos : null);
  }
}
