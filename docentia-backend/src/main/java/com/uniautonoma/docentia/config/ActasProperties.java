package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.entity.Rol;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Configuration;

/**
 * Reglas configurables de compromisos y actas.
 *
 * @param compromisos con una nota menor al umbral el acta exige al menos un compromiso con plazo
 * @param actas rol que puede corregir un acta firmada y si la firma exige el trazo dibujado
 */
@ConfigurationProperties(prefix = "docentia")
public record ActasProperties(@DefaultValue Compromisos compromisos, @DefaultValue Actas actas) {

  public record Compromisos(@DefaultValue("3.8") BigDecimal umbral) {}

  /**
   * @param exigirFirmaDibujada la firma lleva el trazo hecho en la página, que va al PDF
   * @param rotuloDocumento cómo se llama en el acta el código del docente ({@code
   *     usuarios.codigo}); vacío, el acta no lo guarda ni lo imprime
   * @param formatoCodigo código del formato institucional del acta; vacío mientras no exista
   * @param formatoVersion versión de ese formato
   */
  public record Actas(
      @DefaultValue("VICERRECTOR") Rol rolCorrector,
      @DefaultValue("true") boolean exigirFirmaDibujada,
      @DefaultValue("Código") String rotuloDocumento,
      String formatoCodigo,
      String formatoVersion) {

    public Actas {
      rotuloDocumento = vacioANulo(rotuloDocumento);
      formatoCodigo = vacioANulo(formatoCodigo);
      formatoVersion = vacioANulo(formatoVersion);
    }

    private static String vacioANulo(String texto) {
      return texto == null || texto.isBlank() ? null : texto.strip();
    }
  }

  @Configuration
  @EnableConfigurationProperties(ActasProperties.class)
  static class Registro {}
}
