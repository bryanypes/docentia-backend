package com.uniautonoma.docentia.service.analisis.local;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Léxico versionado del modelo local, leído de un archivo de texto. */
public final class Lexico {

  /** Un término: raíz de una palabra, palabra exacta (con "$") o frase. */
  public record Termino(String texto, boolean exacto, double valor) {

    boolean esFrase() {
      return texto.contains(" ");
    }
  }

  private final String version;
  private final Map<String, String> correcciones = new HashMap<>();
  private final List<Termino> negaciones = new ArrayList<>();
  private final List<Termino> intensificadores = new ArrayList<>();
  private final List<Termino> ironias = new ArrayList<>();
  private final List<Termino> positivos = new ArrayList<>();
  private final List<Termino> negativos = new ArrayList<>();
  private final List<Termino> inyecciones = new ArrayList<>();
  private final Map<String, List<Termino>> descriptores = new LinkedHashMap<>();

  private Lexico(String version) {
    this.version = version;
  }

  public static Lexico cargar(String recurso) {
    InputStream entrada = Lexico.class.getClassLoader().getResourceAsStream(recurso);
    if (entrada == null) {
      throw new IllegalStateException("No existe el léxico " + recurso);
    }
    try (BufferedReader lector =
        new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
      String version = null;
      List<String[]> filas = new ArrayList<>();
      for (String linea; (linea = lector.readLine()) != null; ) {
        String limpia = linea.strip();
        if (limpia.startsWith("# version:")) {
          version = limpia.substring("# version:".length()).strip();
        }
        if (limpia.isEmpty() || limpia.startsWith("#")) {
          continue;
        }
        String[] partes = limpia.split("\\s*\\|\\s*");
        filas.add(partes);
      }
      if (version == null) {
        throw new IllegalStateException("El léxico no declara su versión");
      }
      Lexico lexico = new Lexico(version);
      filas.forEach(lexico::agregar);
      return lexico;
    } catch (IOException fallo) {
      throw new UncheckedIOException(fallo);
    }
  }

  private void agregar(String[] partes) {
    String seccion = partes[0];
    String bruto = partes[1];
    boolean exacto = bruto.endsWith("$");
    String texto = exacto ? bruto.substring(0, bruto.length() - 1) : bruto;
    double valor =
        partes.length > 2 && !seccion.equals("CORRECCION") ? Double.parseDouble(partes[2]) : 1;
    Termino termino = new Termino(texto, exacto, valor);
    switch (seccion) {
      case "CORRECCION" -> correcciones.put(texto, partes[2]);
      case "NEGACION" -> negaciones.add(termino);
      case "INTENSIFICADOR" -> intensificadores.add(termino);
      case "IRONIA" -> ironias.add(termino);
      case "POSITIVO" -> positivos.add(termino);
      case "NEGATIVO" -> negativos.add(termino);
      case "INYECCION" -> inyecciones.add(termino);
      default -> {
        if (!seccion.matches("D\\d{2}")) {
          throw new IllegalStateException("Sección desconocida en el léxico: " + seccion);
        }
        descriptores.computeIfAbsent(seccion, s -> new ArrayList<>()).add(termino);
      }
    }
  }

  public String version() {
    return version;
  }

  public Map<String, String> correcciones() {
    return correcciones;
  }

  public List<Termino> negaciones() {
    return negaciones;
  }

  public List<Termino> intensificadores() {
    return intensificadores;
  }

  public List<Termino> ironias() {
    return ironias;
  }

  public List<Termino> positivos() {
    return positivos;
  }

  public List<Termino> negativos() {
    return negativos;
  }

  public List<Termino> inyecciones() {
    return inyecciones;
  }

  public Map<String, List<Termino>> descriptores() {
    return descriptores;
  }
}
