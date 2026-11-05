package com.uniautonoma.docentia.service.analisis.local;

import com.uniautonoma.docentia.service.analisis.EstimadorTokens;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ComentarioAnonimo;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Comentario;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Etiqueta;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Etiquetado;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Elige qué comentarios ve el modelo de lenguaje. El modelo local ya leyó y contó todos; al de
 * lenguaje le basta una muestra representativa, así los tokens casi no crecen con el número de
 * comentarios.
 *
 * <ol>
 *   <li>Quita saludos y firmas, descarta lo que queda sin contenido y los casi duplicados.
 *   <li>Agrupa por estrato (descriptor y polaridad de la primera etiqueta, o el sentimiento si no
 *       tiene) y da al menos un lugar a cada estrato, para no perder los casos poco frecuentes.
 *   <li>Reparte el resto en proporción al tamaño de cada estrato (restos mayores).
 *   <li>Dentro de cada estrato toma en orden de huella del texto: la muestra es reproducible y no
 *       depende de fechas ni del orden de llegada.
 *   <li>Recorta cada comentario en una frontera de palabra y, si la entrada estimada no cabe en el
 *       tope de tokens, achica la muestra (nunca la instrucción).
 * </ol>
 */
public final class MuestreoParaModelo {

  static final String MARCA_RECORTE = "…";
  private static final int SOBRECARGA_POR_COMENTARIO = 4;
  private static final int SOBRECARGA_LEGADO = 12;

  private static final Pattern SALUDO =
      Pattern.compile(
          "^(hola|holi|buenas( tardes| noches)?|buenos d[ií]as|buen d[ií]a|saludos|"
              + "querid[oa] profe(sor|sora)?|profe(sor|sora)?|estimad[oa] profe(sor|sora)?)"
              + "[\\s,.!:;]+",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  private static final Pattern FIRMA =
      Pattern.compile(
          "[\\s,.!;]*(muchas gracias|gracias|saludos|atentamente|att?e?\\.?)( [^.!?]{0,30})?[.!]*$",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /**
   * @param enviados comentarios que van al modelo, en orden de huella
   * @param disponibles comentarios útiles que analizó el modelo local
   * @param sinContenido descartados por quedar vacíos tras quitar saludos o por casi duplicados
   * @param recortados comentarios enviados que se recortaron
   * @param tokensEstimados entrada estimada de la llamada
   * @param tokensSinTopes entrada estimada si se mandaran todos completos, como antes
   */
  public record Muestra(
      List<ComentarioAnonimo> enviados,
      int disponibles,
      int sinContenido,
      int recortados,
      int tokensEstimados,
      int tokensSinTopes) {

    public boolean muestreado() {
      return enviados.size() < disponibles;
    }
  }

  private final int maxComentarios;
  private final int maxCaracteres;
  private final int maxTokensEntrada;

  public MuestreoParaModelo(int maxComentarios, int maxCaracteres, int maxTokensEntrada) {
    this.maxComentarios = Math.max(1, maxComentarios);
    this.maxCaracteres = Math.max(40, maxCaracteres);
    this.maxTokensEntrada = maxTokensEntrada;
  }

  /**
   * @param utiles comentarios útiles, ya anonimizados
   * @param etiquetados lo que dijo el modelo local de cada uno
   * @param instruccion texto fijo que va con la llamada
   * @param temas temas compactos que acompañan la llamada
   */
  public Muestra elegir(
      List<Comentario> utiles,
      List<Etiquetado> etiquetados,
      String instruccion,
      List<String> temas) {
    Map<Long, Etiquetado> porId = new LinkedHashMap<>();
    etiquetados.forEach(e -> porId.putIfAbsent(e.id(), e));
    int fijo =
        EstimadorTokens.estimar(instruccion)
            + temas.stream().mapToInt(t -> EstimadorTokens.estimar(t) + 2).sum()
            + 10;
    long caracteresSinTopes = 0;
    for (Comentario c : utiles) {
      caracteresSinTopes += c.texto().length();
    }
    int tokensSinTopes =
        fijo
            + EstimadorTokens.estimarCaracteres(caracteresSinTopes)
            + utiles.size() * SOBRECARGA_LEGADO;

    List<Comentario> candidatos = new ArrayList<>();
    Set<String> vistos = new HashSet<>();
    int sinContenido = 0;
    List<Comentario> ordenados = new ArrayList<>(utiles);
    ordenados.sort(
        Comparator.comparing((Comentario c) -> PipelineAnalisis.hash(c.texto()))
            .thenComparing(Comentario::id));
    for (Comentario c : ordenados) {
      String limpio = limpiar(c.texto());
      String clave = NormalizadorTexto.comparable(limpio).strip();
      if (NormalizadorTexto.palabrasReales(limpio) < 3 || !vistos.add(clave)) {
        sinContenido++;
        continue;
      }
      candidatos.add(new Comentario(c.id(), limpio));
    }

    int k = Math.min(maxComentarios, candidatos.size());
    while (true) {
      List<Comentario> elegidos = estratificar(candidatos, porId, k);
      List<ComentarioAnonimo> enviados = new ArrayList<>(elegidos.size());
      int recortados = 0;
      int tokens = fijo;
      for (Comentario c : elegidos) {
        String texto = recortar(c.texto());
        if (!texto.equals(c.texto())) {
          recortados++;
        }
        tokens += EstimadorTokens.estimar(texto) + SOBRECARGA_POR_COMENTARIO;
        enviados.add(new ComentarioAnonimo(c.id(), texto));
      }
      if (tokens <= maxTokensEntrada || k <= 1) {
        return new Muestra(
            List.copyOf(enviados), utiles.size(), sinContenido, recortados, tokens, tokensSinTopes);
      }
      k = Math.max(1, (int) Math.floor(k * 0.9));
    }
  }

  private static List<Comentario> estratificar(
      List<Comentario> candidatos, Map<Long, Etiquetado> porId, int k) {
    if (k >= candidatos.size()) {
      return candidatos;
    }
    Map<String, List<Comentario>> estratos = new TreeMap<>();
    for (Comentario c : candidatos) {
      estratos.computeIfAbsent(estrato(porId.get(c.id())), e -> new ArrayList<>()).add(c);
    }
    List<String> claves = new ArrayList<>(estratos.keySet());
    // Si hay más estratos que lugares, ganan los más grandes.
    claves.sort(
        Comparator.comparingInt((String e) -> -estratos.get(e).size())
            .thenComparing(Comparator.naturalOrder()));
    Map<String, Integer> cupo = new LinkedHashMap<>();
    int asignados = 0;
    for (String clave : claves) {
      if (asignados < k) {
        cupo.put(clave, 1);
        asignados++;
      } else {
        cupo.put(clave, 0);
      }
    }
    int resto = k - asignados;
    int capacidad = 0;
    for (String clave : claves) {
      capacidad += estratos.get(clave).size() - cupo.get(clave);
    }
    if (resto > 0 && capacidad > 0) {
      Map<String, Double> restos = new LinkedHashMap<>();
      int repartidos = 0;
      for (String clave : claves) {
        double ideal = resto * (estratos.get(clave).size() - cupo.get(clave)) / (double) capacidad;
        int entero = (int) Math.floor(ideal);
        cupo.merge(clave, entero, Integer::sum);
        repartidos += entero;
        restos.put(clave, ideal - entero);
      }
      List<String> porResto = new ArrayList<>(claves);
      porResto.sort(
          Comparator.comparingDouble((String e) -> -restos.get(e))
              .thenComparing(Comparator.naturalOrder()));
      for (String clave : porResto) {
        if (repartidos >= resto) {
          break;
        }
        if (cupo.get(clave) < estratos.get(clave).size()) {
          cupo.merge(clave, 1, Integer::sum);
          repartidos++;
        }
      }
    }
    List<Comentario> elegidos = new ArrayList<>(k);
    for (String clave : claves) {
      List<Comentario> del = estratos.get(clave);
      elegidos.addAll(del.subList(0, Math.min(del.size(), cupo.get(clave))));
    }
    elegidos.sort(
        Comparator.comparing((Comentario c) -> PipelineAnalisis.hash(c.texto()))
            .thenComparing(Comentario::id));
    return elegidos;
  }

  /** Descriptor y polaridad de la etiqueta principal, o el sentimiento si no tiene. */
  static String estrato(Etiquetado etiquetado) {
    if (etiquetado == null) {
      return "SIN_ETIQUETA";
    }
    return etiquetado.etiquetas().stream()
        .min(Comparator.comparing(Etiqueta::descriptor).thenComparing(Etiqueta::polaridad))
        .map(e -> e.descriptor() + "|" + e.polaridad())
        .orElse("SIN_TEMA|" + etiquetado.sentimiento());
  }

  /** Quita saludos al inicio y firmas al final, y normaliza espacios. */
  static String limpiar(String texto) {
    String limpio = texto.strip().replaceAll("\\s+", " ");
    String anterior;
    do {
      anterior = limpio;
      limpio = SALUDO.matcher(limpio).replaceFirst("").strip();
    } while (!limpio.equals(anterior));
    String sinFirma = FIRMA.matcher(limpio).replaceFirst("").strip();
    return NormalizadorTexto.palabrasReales(sinFirma) >= 3 ? sinFirma : limpio;
  }

  /** Recorta en la última frontera de palabra antes del máximo y marca el recorte. */
  String recortar(String texto) {
    if (texto.length() <= maxCaracteres) {
      return texto;
    }
    int corte = texto.lastIndexOf(' ', maxCaracteres - 1);
    if (corte < maxCaracteres / 2) {
      corte = maxCaracteres - 1;
    }
    return texto.substring(0, corte).strip() + MARCA_RECORTE;
  }
}
