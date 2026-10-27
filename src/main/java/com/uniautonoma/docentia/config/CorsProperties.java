package com.uniautonoma.docentia.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Orígenes del navegador que pueden llamar al API desde otro dominio.
 *
 * @param origenesPermitidos orígenes exactos (esquema, dominio y puerto), por ejemplo {@code
 *     https://docentia.uniautonoma.edu.co}. Vacío por defecto: sin CORS, el frontend debe servirse
 *     desde el mismo origen o detrás de un proxy (como Vite en desarrollo).
 */
@ConfigurationProperties(prefix = "docentia.cors")
public record CorsProperties(@DefaultValue List<String> origenesPermitidos) {

  public CorsProperties {
    origenesPermitidos =
        origenesPermitidos.stream().map(String::trim).filter(origen -> !origen.isEmpty()).toList();
    // Las peticiones llevan el token en Authorization: abrirlas a cualquier origen no es aceptable.
    if (origenesPermitidos.stream().anyMatch(origen -> origen.contains("*"))) {
      throw new IllegalArgumentException(
          "docentia.cors.origenes-permitidos no admite comodines; lista los orígenes exactos");
    }
  }
}
