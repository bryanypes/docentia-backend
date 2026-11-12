package com.uniautonoma.docentia.service.analisis.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Filtro defensivo, aunque el formulario ya valide: deja fuera lo vacío, lo que no tiene texto
 * útil, el spam repetido y los intentos de dar órdenes al modelo, y recorta lo demasiado largo.
 */
public final class FiltroCalidad {

  public static final int LONGITUD_MAXIMA = 1000;

  public record Evaluado(Long id, String texto) {}

  public record Resultado(List<Evaluado> utiles, Map<String, Integer> excluidos, int truncados) {}

  private final Lexico lexico;
  private final int maximoRepeticiones;

  public FiltroCalidad(Lexico lexico, int maximoRepeticiones) {
    this.lexico = lexico;
    this.maximoRepeticiones = maximoRepeticiones;
  }

  public Resultado filtrar(List<Evaluado> entrada) {
    List<Evaluado> utiles = new ArrayList<>();
    Map<String, Integer> excluidos = new LinkedHashMap<>();
    Map<String, Integer> vistos = new HashMap<>();
    int truncados = 0;
    for (Evaluado comentario : entrada) {
      String texto = NormalizadorTexto.limpiar(comentario.texto());
      String motivo = motivo(texto);
      if (motivo == null) {
        int veces = vistos.merge(NormalizadorTexto.comparable(texto), 1, Integer::sum);
        if (veces > maximoRepeticiones) {
          motivo = "DUPLICADO";
        }
      }
      if (motivo != null) {
        excluidos.merge(motivo, 1, Integer::sum);
        continue;
      }
      if (texto.length() > LONGITUD_MAXIMA) {
        texto = texto.substring(0, LONGITUD_MAXIMA).strip();
        truncados++;
      }
      utiles.add(new Evaluado(comentario.id(), texto));
    }
    return new Resultado(List.copyOf(utiles), excluidos, truncados);
  }

  private String motivo(String texto) {
    if (texto.isBlank()) {
      return "VACIO";
    }
    if (texto.codePoints().noneMatch(Character::isLetter)) {
      return "SIN_TEXTO";
    }
    if (NormalizadorTexto.palabrasReales(texto) < 3) {
      return "MUY_CORTO";
    }
    String comparable = " " + NormalizadorTexto.comparable(texto) + " ";
    boolean inyeccion =
        lexico.inyecciones().stream()
            .anyMatch(
                t ->
                    comparable.contains(" " + t.texto() + " ")
                        || comparable.contains(" " + t.texto()));
    return inyeccion ? "INSTRUCCION_SOSPECHOSA" : null;
  }
}
