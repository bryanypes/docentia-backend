package com.uniautonoma.docentia.service.analisis;

import java.time.Duration;
import java.util.List;

/**
 * Modelo de lenguaje que propone hallazgos a partir de comentarios ya filtrados y anonimizados. La
 * implementación se elige con docentia.ia.proveedor.
 */
public interface ModeloIA {

  /**
   * Pide el análisis.
   *
   * @return el texto que devolvió el modelo (se espera JSON) y el modelo que lo generó
   * @throws ModeloIAException si el proveedor no responde, responde con error o se agota el tiempo
   */
  RespuestaModelo analizar(PeticionAnalisis peticion);

  /**
   * Tipo de modelo para rotular el resultado con honestidad: REGLAS si no hay un modelo de lenguaje
   * detrás, LLM si lo hay (o si se simula uno en pruebas).
   */
  default String tipo() {
    return "LLM";
  }

  /** Comentario listo para enviar: sin datos identificables. */
  record ComentarioAnonimo(Long id, String texto) {}

  /**
   * Lo que se envía al modelo.
   *
   * @param instruccion texto de instrucción versionado
   * @param comentarios comentarios anonimizados con su identificador para citarlos
   * @param temas temas que ya contó el modelo local, en forma compacta (D05+4), para que el modelo
   *     de lenguaje los valide o refine
   */
  record PeticionAnalisis(
      String instruccion, List<ComentarioAnonimo> comentarios, List<String> temas) {

    public PeticionAnalisis(String instruccion, List<ComentarioAnonimo> comentarios) {
      this(instruccion, comentarios, List.of());
    }
  }

  /**
   * Tokens que informó el proveedor.
   *
   * @param cache tokens de entrada servidos desde la caché del proveedor, si lo informa
   * @param estimado verdadero si el proveedor no informó el uso y se estimó por caracteres
   */
  record Uso(int entrada, int salida, int cache, boolean estimado) {}

  /**
   * @param uso nulo si el modelo no consume tokens (local o simulado)
   */
  record RespuestaModelo(String contenido, String modelo, Uso uso) {

    public RespuestaModelo(String contenido, String modelo) {
      this(contenido, modelo, null);
    }
  }

  /** Cómo falló el proveedor; decide si se reintenta y qué se le dice al usuario. */
  enum ClaseFallo {
    /** 400, 401, 403, 404 y similares: repetir no sirve, hay que corregir la configuración. */
    CONFIGURACION,
    /** 408, 429, 5xx, tiempo agotado o red: puede funcionar si se espera un poco. */
    TRANSITORIO,
    /** El proveedor cortó la respuesta por el tope de tokens. */
    CORTADA,
    /** Respondió, pero sin el contenido esperado. */
    INVALIDA
  }

  /**
   * El proveedor falló; el mensaje es técnico y solo va al registro, no al usuario.
   *
   * <p>{@code esperaSugerida} es el {@code Retry-After} del proveedor, si lo mandó; {@code uso} son
   * los tokens que se gastaron aunque fallara (una respuesta cortada sí consume).
   */
  class ModeloIAException extends RuntimeException {

    private final ClaseFallo clase;
    private final transient Duration esperaSugerida;
    private final transient Uso uso;

    /** Sin clase: se trata como transitorio, lo más prudente para un fallo desconocido. */
    public ModeloIAException(String mensaje, Throwable causa) {
      this(mensaje, causa, ClaseFallo.TRANSITORIO, null, null);
    }

    public ModeloIAException(String mensaje, Throwable causa, ClaseFallo clase) {
      this(mensaje, causa, clase, null, null);
    }

    public ModeloIAException(
        String mensaje, Throwable causa, ClaseFallo clase, Duration esperaSugerida, Uso uso) {
      super(mensaje, causa);
      this.clase = clase;
      this.esperaSugerida = esperaSugerida;
      this.uso = uso;
    }

    public ClaseFallo clase() {
      return clase;
    }

    /** Lo que pidió esperar el proveedor, o nulo. */
    public Duration esperaSugerida() {
      return esperaSugerida;
    }

    /** Tokens gastados en el intento fallido, o nulo si no se sabe. */
    public Uso uso() {
      return uso;
    }
  }
}
