package com.uniautonoma.docentia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del análisis de comentarios con un modelo de lenguaje.
 *
 * @param habilitado si es falso no se aceptan solicitudes; por defecto apagado fuera de dev porque
 *     enviar comentarios a un proveedor externo exige revisar la política de datos
 * @param proveedor "simulado" (determinista, sin red) o "chat-completions" (API compatible con el
 *     formato chat/completions)
 * @param urlBase dirección del proveedor, sin barra final
 * @param modelo identificador del modelo en el proveedor
 * @param timeoutSegundos tiempo máximo de espera de la respuesta
 * @param clave clave del proveedor; solo por variable de entorno (DOCENTIA_IA_CLAVE)
 * @param minimoEncuestas encuestas cerradas necesarias para generar un borrador
 */
@ConfigurationProperties(prefix = "docentia.ia")
public record IaProperties(
    @DefaultValue("false") boolean habilitado,
    @DefaultValue("simulado") String proveedor,
    String urlBase,
    String modelo,
    @DefaultValue("60") int timeoutSegundos,
    String clave,
    @DefaultValue("10") int minimoEncuestas,
    @DefaultValue Worker worker) {

  /**
   * Procesador en segundo plano.
   *
   * @param habilitado si es falso nadie toma las solicitudes de la cola (las pruebas lo apagan y lo
   *     invocan a mano)
   * @param intervaloMs pausa entre revisiones de la cola
   */
  public record Worker(
      @DefaultValue("false") boolean habilitado, @DefaultValue("5000") long intervaloMs) {}
}
