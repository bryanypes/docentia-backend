package com.uniautonoma.docentia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Topes, caché y presupuesto del proveedor externo. Por qué cada valor por defecto está en
 * docs/ANALISIS_IA_COSTOS.md. Un presupuesto en 0 queda desactivado.
 *
 * @param maxComentariosLlm comentarios que se envían como máximo por análisis (muestreo si hay más)
 * @param maxCaracteresComentario largo máximo de cada comentario enviado; se recorta en una palabra
 * @param maxTokensEntrada tope estimado de tokens de entrada por llamada
 * @param maxTokensSalida tope de tokens de salida que se le pide al proveedor
 * @param presupuestoTokensDia tokens por día (entrada + salida); 0 = sin tope
 * @param presupuestoTokensPeriodo tokens por periodo académico; 0 = sin tope
 * @param enfriamientoRegeneracionMinutos espera mínima entre regeneraciones de un docente y periodo
 * @param periodosReferencia periodos anteriores para las tendencias (1 a 3)
 * @param precioMillonTokensEntrada precio por millón de tokens de entrada; vacío = no se calcula
 * @param precioMillonTokensSalida precio por millón de tokens de salida; vacío = no se calcula
 */
@ConfigurationProperties(prefix = "docentia.ia")
public record AhorroIaProperties(
    @DefaultValue("150") int maxComentariosLlm,
    @DefaultValue("300") int maxCaracteresComentario,
    @DefaultValue("16000") int maxTokensEntrada,
    @DefaultValue("1500") int maxTokensSalida,
    @DefaultValue("500000") long presupuestoTokensDia,
    @DefaultValue("3000000") long presupuestoTokensPeriodo,
    @DefaultValue("60") int enfriamientoRegeneracionMinutos,
    @DefaultValue("1") int periodosReferencia,
    Double precioMillonTokensEntrada,
    Double precioMillonTokensSalida) {

  /** Periodos de referencia dentro de 1 a 3. */
  public int periodosReferenciaAcotados() {
    return Math.max(1, Math.min(3, periodosReferencia));
  }
}
