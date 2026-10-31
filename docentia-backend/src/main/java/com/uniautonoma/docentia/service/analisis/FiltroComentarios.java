package com.uniautonoma.docentia.service.analisis;

import java.util.Optional;

/**
 * Decide si un comentario se excluye del análisis por ofensivo o por hablar de rasgos personales
 * del docente. Se aplica antes de armar la petición al modelo.
 *
 * <p>Ningún filtro automático es perfecto: puede dejar pasar algo y, en casos raros, excluir algo
 * válido. Por eso cada hallazgo del borrador lo valida un coordinador (E4) antes de que el docente
 * lo vea.
 */
public interface FiltroComentarios {

  /**
   * @return el motivo si el comentario debe excluirse, o vacío si puede enviarse
   */
  Optional<Exclusion> evaluar(String comentario);

  /**
   * @param categoria INSULTO, APARIENCIA, EDAD, GENERO, VIDA_PRIVADA u otra definida en las reglas
   * @param regla la regla que lo excluyó, tal como está escrita en el archivo
   */
  record Exclusion(String categoria, String regla) {}
}
