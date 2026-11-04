package com.uniautonoma.docentia.security;

import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Quién ve el análisis de comentarios, los comentarios anonimizados y el resumen agregado. Es el
 * único lugar que hay que tocar para abrirlo a otro rol (por ejemplo un administrador o un
 * visualizador): los endpoints la usan con {@code @PreAuthorize("@politicaComentarios...")}.
 *
 * <p>El docente nunca está: ve solo los hallazgos que coordinación validó y publicó (A4). El
 * estudiante tampoco. El alcance por facultad se aplica aparte, en el servicio.
 */
@Component("politicaComentarios")
public class PoliticaComentarios {

  static final Set<String> ANALISIS = Set.of("ROLE_COORDINADOR", "ROLE_VICERRECTOR");
  static final Set<String> COMENTARIOS = Set.of("ROLE_COORDINADOR", "ROLE_VICERRECTOR");
  static final Set<String> RESUMEN = Set.of("ROLE_COORDINADOR", "ROLE_VICERRECTOR");

  public boolean puedeVerAnalisis(Authentication autenticacion) {
    return tieneAlguno(autenticacion, ANALISIS);
  }

  public boolean puedeVerComentarios(Authentication autenticacion) {
    return tieneAlguno(autenticacion, COMENTARIOS);
  }

  public boolean puedeVerResumen(Authentication autenticacion) {
    return tieneAlguno(autenticacion, RESUMEN);
  }

  private static boolean tieneAlguno(Authentication autenticacion, Set<String> permitidas) {
    return autenticacion != null
        && autenticacion.isAuthenticated()
        && autenticacion.getAuthorities().stream()
            .anyMatch(autoridad -> permitidas.contains(autoridad.getAuthority()));
  }
}
