package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.entity.CategoriaDesempeno;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Clasifica una nota final en la escala de la Resolución 074. La nota se redondea a dos decimales
 * antes de clasificar, para que la categoría coincida con el número que ve el docente.
 */
public final class ClasificadorDesempeno {

  private static final BigDecimal MINIMO = BigDecimal.ZERO;
  private static final BigDecimal MAXIMO = new BigDecimal("5");
  private static final BigDecimal DESDE_MUY_ALTO = new BigDecimal("4.60");
  private static final BigDecimal DESDE_ALTO = new BigDecimal("3.80");
  private static final BigDecimal DESDE_ACEPTABLE = new BigDecimal("3.00");
  private static final BigDecimal DESDE_BAJO = new BigDecimal("2.20");

  private ClasificadorDesempeno() {}

  /**
   * Clasifica una nota.
   *
   * @param nota nota final de 0 a 5
   * @return la categoría que le corresponde
   * @throws DatosInvalidosException si la nota es nula o está fuera de la escala
   */
  public static CategoriaDesempeno clasificar(BigDecimal nota) {
    if (nota == null || nota.compareTo(MINIMO) < 0 || nota.compareTo(MAXIMO) > 0) {
      throw new DatosInvalidosException("La nota debe estar entre 0 y 5");
    }
    BigDecimal redondeada = nota.setScale(2, RoundingMode.HALF_UP);
    if (redondeada.compareTo(DESDE_MUY_ALTO) >= 0) {
      return CategoriaDesempeno.MUY_ALTO;
    }
    if (redondeada.compareTo(DESDE_ALTO) >= 0) {
      return CategoriaDesempeno.ALTO;
    }
    if (redondeada.compareTo(DESDE_ACEPTABLE) >= 0) {
      return CategoriaDesempeno.ACEPTABLE;
    }
    if (redondeada.compareTo(DESDE_BAJO) >= 0) {
      return CategoriaDesempeno.BAJO;
    }
    return CategoriaDesempeno.MUY_BAJO;
  }
}
