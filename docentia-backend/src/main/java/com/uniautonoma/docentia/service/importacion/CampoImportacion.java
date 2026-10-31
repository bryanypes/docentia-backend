package com.uniautonoma.docentia.service.importacion;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Campos que el sistema espera del CSV de evaluación de estudiantes. Cada columna del archivo se
 * asigna a lo sumo a uno de estos campos; las columnas sin asignar se ignoran.
 */
public enum CampoImportacion {
  CORREO_DOCENTE(
      "Correo del docente",
      true,
      "correo",
      "correo docente",
      "correo del docente",
      "correo electronico",
      "email",
      "email docente"),
  DOMINIO_DISCIPLINAR("Dominio disciplinar", true),
  PLANEACION_ACADEMICA("Planeación académica", true),
  METODOLOGIA_ENSENANZA("Metodología de enseñanza", true, "metodologia de ensenanza"),
  EVALUACION_APRENDIZAJE("Evaluación del aprendizaje", true, "evaluacion del aprendizaje"),
  COMPROMISO_INSTITUCIONAL("Compromiso institucional", true),
  COMENTARIO(
      "Comentario", false, "comentarios", "comentario estudiante", "observacion", "observaciones");

  /** Los cinco aspectos, en el orden de NotasAspectos. */
  public static final List<CampoImportacion> ASPECTOS =
      List.of(
          DOMINIO_DISCIPLINAR,
          PLANEACION_ACADEMICA,
          METODOLOGIA_ENSENANZA,
          EVALUACION_APRENDIZAJE,
          COMPROMISO_INSTITUCIONAL);

  private final String etiqueta;
  private final boolean obligatorio;
  private final Set<String> nombresReconocidos;

  CampoImportacion(String etiqueta, boolean obligatorio, String... sinonimos) {
    this.etiqueta = etiqueta;
    this.obligatorio = obligatorio;
    this.nombresReconocidos =
        Stream.concat(Stream.of(name(), etiqueta), Arrays.stream(sinonimos))
            .map(CampoImportacion::normalizar)
            .collect(Collectors.toUnmodifiableSet());
  }

  public String getEtiqueta() {
    return etiqueta;
  }

  public boolean isObligatorio() {
    return obligatorio;
  }

  /**
   * Dice si el encabezado de una columna corresponde a este campo, sin importar tildes ni signos.
   */
  public boolean reconoce(String encabezado) {
    return nombresReconocidos.contains(normalizar(encabezado));
  }

  /** Minúsculas, sin tildes y con cualquier signo convertido en un solo espacio. */
  public static String normalizar(String texto) {
    String sinTildes =
        Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
    return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
  }
}
