package com.uniautonoma.docentia.service.analisis.local;

import com.uniautonoma.docentia.service.analisis.local.CatalogoDescriptores.Descriptor;
import com.uniautonoma.docentia.service.analisis.local.Lexico.Termino;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Advertencia;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Aspectos;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Cita;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Criterio;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Sentimiento;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Tema;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Tendencia;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modelo local basado en reglas: léxico de polaridad, negaciones, intensificadores y descriptores
 * de la Res. 074. Es determinista, no usa red y no es un modelo generativo.
 */
public class AnalizadorLocal implements AnalizadorComentarios {

  public static final String VERSION = "reglas-1.0";
  private static final double UMBRAL_POLARIDAD = 0.25;
  private static final int VENTANA_NEGACION = 3;
  private static final Pattern SEPARADOR =
      Pattern.compile("(?iu)[.!?;\\n]+|\\s+(pero|aunque|sin embargo|mas no|en cambio)\\s+");

  private final Lexico lexico;
  private final ParametrosAnalisis parametros;

  public AnalizadorLocal(Lexico lexico, ParametrosAnalisis parametros) {
    this.lexico = lexico;
    this.parametros = parametros;
  }

  public record ParametrosAnalisis(
      int minimoRecurrencia,
      double porcentajeRecurrencia,
      int maximoPorLista,
      double umbralTendencia,
      int minimoRespuestas) {}

  private record Clausula(String original, double puntaje, Set<String> descriptores) {}

  @Override
  public Salida analizar(List<Comentario> comentarios, ResultadoAnalisis anterior) {
    List<Etiquetado> etiquetados = new ArrayList<>();
    Map<String, List<Etiqueta>> porTema = new TreeMap<>();
    Map<String, List<Long>> idsPorTema = new HashMap<>();
    Map<String, Map<Long, String>> fragmentos = new HashMap<>();
    int positivos = 0;
    int negativos = 0;
    int neutros = 0;
    int mixtos = 0;
    Map<String, int[]> porCriterio = new TreeMap<>();

    for (Comentario comentario : comentarios) {
      List<Clausula> clausulas = clausulas(comentario.texto());
      boolean hayPositiva = false;
      boolean hayNegativa = false;
      Map<String, Etiqueta> etiquetas = new LinkedHashMap<>();
      for (Clausula clausula : clausulas) {
        String polaridad = polaridad(clausula.puntaje());
        hayPositiva |= polaridad.equals("POSITIVO");
        hayNegativa |= polaridad.equals("NEGATIVO");
        for (String codigo : clausula.descriptores()) {
          Descriptor descriptor = CatalogoDescriptores.DESCRIPTORES.get(codigo);
          Etiqueta etiqueta =
              new Etiqueta(descriptor.criterio(), codigo, polaridad, Math.abs(clausula.puntaje()));
          String clave = codigo + "|" + polaridad;
          if (etiquetas.putIfAbsent(clave, etiqueta) == null) {
            porTema.computeIfAbsent(clave, k -> new ArrayList<>()).add(etiqueta);
            idsPorTema.computeIfAbsent(clave, k -> new ArrayList<>()).add(comentario.id());
            fragmentos
                .computeIfAbsent(clave, k -> new LinkedHashMap<>())
                .putIfAbsent(comentario.id(), clausula.original());
          }
        }
      }
      for (Etiqueta etiqueta : etiquetas.values()) {
        int[] conteo = porCriterio.computeIfAbsent(etiqueta.criterio(), k -> new int[3]);
        conteo[
            etiqueta.polaridad().equals("POSITIVO")
                ? 0
                : etiqueta.polaridad().equals("NEGATIVO") ? 1 : 2]++;
      }
      String sentimiento =
          hayPositiva && hayNegativa
              ? "MIXTO"
              : hayPositiva ? "POSITIVO" : hayNegativa ? "NEGATIVO" : "NEUTRO";
      switch (sentimiento) {
        case "MIXTO" -> mixtos++;
        case "POSITIVO" -> positivos++;
        case "NEGATIVO" -> negativos++;
        default -> neutros++;
      }
      etiquetados.add(
          new Etiquetado(comentario.id(), sentimiento, List.copyOf(etiquetas.values())));
    }

    int utiles = comentarios.size();
    int umbral =
        Math.max(
            parametros.minimoRecurrencia(),
            (int) Math.ceil(utiles * parametros.porcentajeRecurrencia()));
    List<Tema> temas = new ArrayList<>();
    for (Map.Entry<String, List<Etiqueta>> entrada : porTema.entrySet()) {
      String[] clave = entrada.getKey().split("\\|");
      if (clave[1].equals("NEUTRO")) {
        continue;
      }
      Descriptor descriptor = CatalogoDescriptores.DESCRIPTORES.get(clave[0]);
      int frecuencia = entrada.getValue().size();
      double intensidad =
          redondear(
              entrada.getValue().stream().mapToDouble(Etiqueta::intensidad).average().orElse(0));
      List<Cita> citas =
          fragmentos.get(entrada.getKey()).entrySet().stream()
              .limit(2)
              .map(f -> new Cita(f.getKey(), f.getValue()))
              .toList();
      temas.add(
          new Tema(
              descriptor.criterio(),
              descriptor.codigo(),
              descriptor.nombre(),
              clave[1],
              frecuencia,
              porcentaje(frecuencia, utiles),
              intensidad,
              redondear(frecuencia * (1 + intensidad)),
              frecuencia >= umbral,
              citas));
    }
    temas.sort(
        Comparator.comparingDouble(Tema::relevancia)
            .reversed()
            .thenComparing(Tema::descriptor)
            .thenComparing(Tema::polaridad));

    List<Tema> fortalezas = mejores(temas, "POSITIVO");
    List<Tema> oportunidades = mejores(temas, "NEGATIVO");

    List<Criterio> criterios = new ArrayList<>();
    porCriterio.forEach(
        (codigo, conteo) -> {
          int total = conteo[0] + conteo[1];
          criterios.add(
              new Criterio(
                  codigo,
                  CatalogoDescriptores.CRITERIOS.get(codigo),
                  conteo[0],
                  conteo[1],
                  conteo[2],
                  total == 0 ? 0 : redondear((conteo[0] - conteo[1]) / (double) total)));
        });

    List<Advertencia> advertencias = new ArrayList<>();
    if (utiles < parametros.minimoRespuestas()) {
      advertencias.add(
          new Advertencia(
              "POCOS_COMENTARIOS",
              utiles,
              "Hay menos comentarios útiles que el mínimo; los resultados no se muestran."));
    }
    int conPolaridad = positivos + negativos + mixtos;
    double indice =
        conPolaridad == 0 ? 0 : redondear((positivos - negativos) / (double) Math.max(1, utiles));

    Map<String, Object> usados = new LinkedHashMap<>();
    usados.put("umbralRecurrencia", umbral);
    usados.put("porcentajeRecurrencia", parametros.porcentajeRecurrencia());
    usados.put("minimoRecurrencia", parametros.minimoRecurrencia());
    usados.put("maximoPorLista", parametros.maximoPorLista());
    usados.put("umbralTendencia", parametros.umbralTendencia());
    usados.put("minimoRespuestas", parametros.minimoRespuestas());

    ResultadoAnalisis resultado =
        new ResultadoAnalisis(
            "REGLAS",
            VERSION,
            lexico.version(),
            utiles,
            Map.of(),
            new Sentimiento(positivos, negativos, neutros, mixtos, indice),
            List.copyOf(temas),
            new Aspectos(fortalezas, oportunidades),
            tendencias(temas, anterior),
            advertencias,
            criterios,
            usados);
    return new Salida(resultado, etiquetados);
  }

  private List<Tema> mejores(List<Tema> temas, String polaridad) {
    return temas.stream()
        .filter(t -> t.polaridad().equals(polaridad))
        .limit(parametros.maximoPorLista())
        .toList();
  }

  /** Compara contra el periodo anterior del docente; sin referencia válida, no inventa nada. */
  private List<Tendencia> tendencias(List<Tema> actuales, ResultadoAnalisis anterior) {
    boolean conReferencia =
        anterior != null && anterior.nComentarios() >= parametros.minimoRespuestas();
    Map<String, Tema> previos = new LinkedHashMap<>();
    if (conReferencia) {
      anterior.temas().forEach(t -> previos.put(t.descriptor() + "|" + t.polaridad(), t));
    }
    List<Tendencia> tendencias = new ArrayList<>();
    Set<String> vistos = new LinkedHashSet<>();
    for (Tema tema : actuales) {
      String clave = tema.descriptor() + "|" + tema.polaridad();
      vistos.add(clave);
      if (!conReferencia) {
        tendencias.add(
            new Tendencia(
                tema.criterio(),
                tema.descriptor(),
                tema.polaridad(),
                null,
                tema.porcentaje(),
                "SIN_REFERENCIA"));
        continue;
      }
      Tema previo = previos.get(clave);
      double antes = previo == null ? 0 : previo.porcentaje();
      tendencias.add(
          new Tendencia(
              tema.criterio(),
              tema.descriptor(),
              tema.polaridad(),
              antes,
              tema.porcentaje(),
              previo == null ? "NUEVO" : cambio(tema.porcentaje() - antes)));
    }
    if (conReferencia) {
      previos.forEach(
          (clave, previo) -> {
            if (!vistos.contains(clave)) {
              tendencias.add(
                  new Tendencia(
                      previo.criterio(),
                      previo.descriptor(),
                      previo.polaridad(),
                      previo.porcentaje(),
                      0,
                      "EN_DISMINUCION"));
            }
          });
    }
    return tendencias;
  }

  private String cambio(double diferencia) {
    if (diferencia >= parametros.umbralTendencia()) {
      return "EN_AUMENTO";
    }
    if (diferencia <= -parametros.umbralTendencia()) {
      return "EN_DISMINUCION";
    }
    return "SE_MANTIENE";
  }

  private static String polaridad(double puntaje) {
    if (puntaje > UMBRAL_POLARIDAD) {
      return "POSITIVO";
    }
    if (puntaje < -UMBRAL_POLARIDAD) {
      return "NEGATIVO";
    }
    return "NEUTRO";
  }

  /** Parte el comentario en cláusulas que conservan el texto original, para citar textualmente. */
  List<Clausula> clausulas(String texto) {
    List<String> partes = new ArrayList<>();
    Matcher separador = SEPARADOR.matcher(texto);
    int inicio = 0;
    while (separador.find()) {
      partes.add(texto.substring(inicio, separador.start()));
      inicio = separador.end();
    }
    partes.add(texto.substring(inicio));
    List<Clausula> clausulas = new ArrayList<>();
    for (String parte : partes) {
      String original = parte.strip();
      if (original.isEmpty()) {
        continue;
      }
      List<String> tokens = tokens(original);
      clausulas.add(new Clausula(original, puntaje(tokens), descriptores(tokens)));
    }
    return clausulas;
  }

  private List<String> tokens(String texto) {
    List<String> tokens = new ArrayList<>();
    for (String palabra : NormalizadorTexto.comparable(texto).split(" ")) {
      if (palabra.isEmpty()) {
        continue;
      }
      String corregida = lexico.correcciones().getOrDefault(palabra, palabra);
      tokens.addAll(List.of(corregida.split(" ")));
    }
    return tokens;
  }

  private double puntaje(List<String> tokens) {
    String frase = " " + String.join(" ", tokens) + " ";
    double total = 0;
    int negacionRestante = 0;
    double multiplicador = 1;
    boolean[] usados = new boolean[tokens.size()];
    // Frases primero: "buena gente", "perdida de tiempo".
    for (Termino termino : concatenar(lexico.positivos(), lexico.negativos())) {
      if (termino.esFrase() && frase.contains(" " + termino.texto() + " ")) {
        int posicion = posicionFrase(tokens, termino.texto());
        boolean negada = negadaAntes(tokens, posicion);
        double signo = lexico.positivos().contains(termino) ? 1 : -1;
        total += (negada ? -signo : signo) * termino.valor();
        int largo = termino.texto().split(" ").length;
        for (int i = posicion; i < posicion + largo && i < usados.length; i++) {
          usados[i] = true;
        }
      }
    }
    for (int i = 0; i < tokens.size(); i++) {
      String token = tokens.get(i);
      if (i + 1 < tokens.size()
          && hay(lexico.intensificadores(), token + " " + tokens.get(i + 1))) {
        multiplicador = valor(lexico.intensificadores(), token + " " + tokens.get(i + 1));
        i++;
        continue;
      }
      if (hay(lexico.negaciones(), token)) {
        negacionRestante = VENTANA_NEGACION;
        continue;
      }
      if (hay(lexico.intensificadores(), token)) {
        multiplicador = valor(lexico.intensificadores(), token);
        continue;
      }
      if (usados[i]) {
        negacionRestante = Math.max(0, negacionRestante - 1);
        continue;
      }
      double peso = 0;
      Termino positivo = coincide(lexico.positivos(), token);
      Termino negativo = coincide(lexico.negativos(), token);
      if (negativo != null) {
        peso = -negativo.valor();
      } else if (positivo != null) {
        peso = positivo.valor();
      }
      if (peso != 0) {
        if (negacionRestante > 0) {
          peso = -peso;
        }
        total += peso * multiplicador;
        multiplicador = 1;
      }
      negacionRestante = Math.max(0, negacionRestante - 1);
    }
    boolean ironica =
        lexico.ironias().stream().anyMatch(t -> frase.contains(" " + t.texto() + " "));
    if (ironica && total > 0) {
      total = -total;
    }
    return total;
  }

  private boolean negadaAntes(List<String> tokens, int posicion) {
    for (int i = Math.max(0, posicion - VENTANA_NEGACION); i < posicion; i++) {
      if (hay(lexico.negaciones(), tokens.get(i))) {
        return true;
      }
    }
    return false;
  }

  private static int posicionFrase(List<String> tokens, String frase) {
    String[] partes = frase.split(" ");
    for (int i = 0; i + partes.length <= tokens.size(); i++) {
      boolean igual = true;
      for (int j = 0; j < partes.length && igual; j++) {
        igual = tokens.get(i + j).equals(partes[j]);
      }
      if (igual) {
        return i;
      }
    }
    return 0;
  }

  private Set<String> descriptores(List<String> tokens) {
    String frase = " " + String.join(" ", tokens) + " ";
    Set<String> encontrados = new LinkedHashSet<>();
    Map<String, Boolean> soloGenerico = new HashMap<>();
    lexico
        .descriptores()
        .forEach(
            (codigo, terminos) -> {
              for (Termino termino : terminos) {
                boolean coincide =
                    termino.esFrase()
                        ? frase.contains(" " + termino.texto() + " ")
                        : tokens.stream().anyMatch(t -> calza(termino, t));
                if (coincide) {
                  boolean generico = codigo.equals("D06") && termino.texto().startsWith("nota");
                  soloGenerico.merge(codigo, generico, (a, b) -> a && b);
                  encontrados.add(codigo);
                }
              }
            });
    // "Las notas se demoran" habla de retroalimentación, no de lo que se evalúa.
    if (encontrados.contains("D07") && Boolean.TRUE.equals(soloGenerico.get("D06"))) {
      encontrados.remove("D06");
    }
    return encontrados;
  }

  private static boolean calza(Termino termino, String token) {
    return termino.exacto() ? token.equals(termino.texto()) : token.startsWith(termino.texto());
  }

  private static Termino coincide(List<Termino> terminos, String token) {
    for (Termino termino : terminos) {
      if (!termino.esFrase() && calza(termino, token)) {
        return termino;
      }
    }
    return null;
  }

  private static boolean hay(List<Termino> terminos, String texto) {
    return terminos.stream().anyMatch(t -> t.texto().equals(texto));
  }

  private static double valor(List<Termino> terminos, String texto) {
    return terminos.stream().filter(t -> t.texto().equals(texto)).findFirst().orElseThrow().valor();
  }

  private static List<Termino> concatenar(List<Termino> a, List<Termino> b) {
    List<Termino> todos = new ArrayList<>(a);
    todos.addAll(b);
    return todos;
  }

  private static double porcentaje(int parte, int total) {
    return total == 0 ? 0 : redondear(parte * 100.0 / total);
  }

  private static double redondear(double valor) {
    return Math.round(valor * 100) / 100.0;
  }
}
