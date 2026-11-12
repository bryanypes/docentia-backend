package com.uniautonoma.docentia.dto;

import java.util.List;

/**
 * Modelos que se pueden elegir al pedir un análisis, para mostrarlos antes de confirmar. Sin cifras
 * en dinero.
 *
 * @param porDefecto LOCAL o LLM: el que se usa si la solicitud no dice nada
 * @param llmDisponible si hay un modelo de lenguaje configurado en este ambiente
 * @param modeloLlm nombre del modelo externo (sin URL ni clave), o nulo
 * @param maxComentariosLlm comentarios que ve como máximo el modelo de lenguaje por análisis
 * @param tokensPorAnalisis tope de tokens de un análisis nuevo con el modelo de lenguaje
 * @param tokensRestantesHoy presupuesto diario que queda; nulo si no hay tope
 * @param advertencia texto para mostrar antes de confirmar el modelo de lenguaje
 */
public record ModelosAnalisisResponse(
    String porDefecto,
    List<Opcion> opciones,
    boolean llmDisponible,
    String modeloLlm,
    int maxComentariosLlm,
    int maxCaracteresComentario,
    Tokens tokensPorAnalisis,
    Long tokensRestantesHoy,
    String advertencia) {

  /**
   * @param codigo LOCAL o LLM, el valor del campo {@code modelo} al pedir
   * @param consumeTokens si gasta tokens del proveedor
   */
  public record Opcion(
      String codigo,
      String nombre,
      boolean disponible,
      boolean consumeTokens,
      String descripcion) {}

  /**
   * @param entradaMaxima tope de entrada por llamada
   * @param salidaMaxima tope de salida por llamada
   * @param totalMaximo entrada + salida; una respuesta desde la caché gasta 0
   */
  public record Tokens(int entradaMaxima, int salidaMaxima, int totalMaximo) {}
}
