package com.uniautonoma.docentia.service.analisis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Etiquetado;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Salida;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal.ParametrosAnalisis;
import com.uniautonoma.docentia.service.analisis.local.CatalogoDescriptores;
import com.uniautonoma.docentia.service.analisis.local.FiltroCalidad;
import com.uniautonoma.docentia.service.analisis.local.FiltroCalidad.Evaluado;
import com.uniautonoma.docentia.service.analisis.local.Lexico;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis.Preparacion;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Valida el modelo local contra comentarios SINTÉTICOS etiquetados a mano. Las métricas no
 * equivalen a precisión en producción; los umbrales son guardas de regresión fijadas con la primera
 * corrida real.
 */
class ValidacionAnalisisTest {

  static final ParametrosAnalisis PARAMETROS = new ParametrosAnalisis(3, 0.2, 3, 10, 5);

  private static JsonNode datos;
  private static Lexico lexico;
  private static PipelineAnalisis pipeline;

  @BeforeAll
  static void cargar() throws Exception {
    try (InputStream entrada =
        new ClassPathResource("ia/validacion-comentarios.json").getInputStream()) {
      datos = new ObjectMapper().readTree(entrada);
    }
    lexico = Lexico.cargar("ia/lexico-v1.txt");
    pipeline =
        new PipelineAnalisis(
            new FiltroCalidad(lexico, 3),
            new FiltroPorReglas(new ClassPathResource("filtros/reglas-comentarios.txt")),
            new Anonimizador());
  }

  private static List<Evaluado> entrada() {
    List<Evaluado> comentarios = new ArrayList<>();
    for (JsonNode c : datos.path("comentarios")) {
      comentarios.add(new Evaluado(c.path("id").asLong(), c.path("texto").asText()));
    }
    return comentarios;
  }

  private static Salida correr(List<Evaluado> comentarios, Preparacion[] preparacion) {
    Preparacion preparada = pipeline.preparar(comentarios, "Ana María Ruiz");
    preparacion[0] = preparada;
    Salida salida = new AnalizadorLocal(lexico, PARAMETROS).analizar(preparada.utiles(), null);
    return new Salida(pipeline.completar(salida.resultado(), preparada), salida.etiquetados());
  }

  record Metricas(double precision, double exhaustividad, double f1, int soporte) {}

  /** Métricas por criterio a nivel de comentario y exactitud de polaridad. */
  static Map<String, Object> medir() {
    Preparacion[] preparacion = new Preparacion[1];
    Salida salida = correr(entrada(), preparacion);
    Map<Long, Etiquetado> predichos = new HashMap<>();
    salida.etiquetados().forEach(e -> predichos.put(e.id(), e));
    Map<String, int[]> conteo = new TreeMap<>();
    int polaridadOk = 0;
    int polaridadTotal = 0;
    int exclusionOk = 0;
    int exclusionTotal = 0;
    Set<Long> excluidosReales = new HashSet<>();
    for (JsonNode c : datos.path("comentarios")) {
      long id = c.path("id").asLong();
      boolean debeExcluirse = !c.path("excluir").isNull() || !c.path("sensible").isNull();
      Etiquetado predicho = predichos.get(id);
      if (debeExcluirse) {
        exclusionTotal++;
        excluidosReales.add(id);
        if (predicho == null) {
          exclusionOk++;
        }
        continue;
      }
      if (predicho == null) {
        continue;
      }
      Set<String> esperados = new HashSet<>();
      c.path("etiquetas").forEach(e -> esperados.add(criterio(e.path("descriptor").asText())));
      Set<String> obtenidos = new HashSet<>();
      predicho.etiquetas().forEach(e -> obtenidos.add(e.criterio()));
      for (String criterio : List.of("C2", "C3", "C4", "C5")) {
        int[] x = conteo.computeIfAbsent(criterio, k -> new int[3]);
        boolean esperado = esperados.contains(criterio);
        boolean obtenido = obtenidos.contains(criterio);
        if (esperado && obtenido) {
          x[0]++;
        } else if (obtenido) {
          x[1]++;
        } else if (esperado) {
          x[2]++;
        }
      }
      polaridadTotal++;
      if (predicho.sentimiento().equals(c.path("sentimiento").asText())) {
        polaridadOk++;
      }
    }
    Map<String, Object> resultado = new TreeMap<>();
    conteo.forEach(
        (criterio, x) -> {
          double p = x[0] + x[1] == 0 ? 0 : x[0] / (double) (x[0] + x[1]);
          double r = x[0] + x[2] == 0 ? 0 : x[0] / (double) (x[0] + x[2]);
          double f = p + r == 0 ? 0 : 2 * p * r / (p + r);
          resultado.put(criterio, new Metricas(r2(p), r2(r), r2(f), x[0] + x[2]));
        });
    resultado.put("exactitudPolaridad", r2(polaridadOk / (double) polaridadTotal));
    resultado.put("comentariosEvaluados", polaridadTotal);
    resultado.put("exclusionesCorrectas", exclusionOk + " de " + exclusionTotal);
    return resultado;
  }

  private static String criterio(String descriptor) {
    return CatalogoDescriptores.DESCRIPTORES.get(descriptor).criterio();
  }

  private static double r2(double valor) {
    return Math.round(valor * 100) / 100.0;
  }

  @Test
  void lasMetricasNoBajanDeLaPrimeraCorridaReal() {
    Map<String, Object> metricas = medir();
    metricas.forEach((clave, valor) -> System.out.println("METRICA " + clave + " = " + valor));
    // Primera corrida (lexico-2026.11-v1): C2 0,97; C3 0,92; C4 1,00; C5 1,00; polaridad 0,89.
    // El umbral es esa cifra menos 0,05: una regresión del léxico hace fallar la prueba.
    assertThat(((Metricas) metricas.get("C2")).f1()).isGreaterThanOrEqualTo(0.92);
    assertThat(((Metricas) metricas.get("C3")).f1()).isGreaterThanOrEqualTo(0.87);
    assertThat(((Metricas) metricas.get("C4")).f1()).isGreaterThanOrEqualTo(0.95);
    assertThat(((Metricas) metricas.get("C5")).f1()).isGreaterThanOrEqualTo(0.95);
    assertThat((double) metricas.get("exactitudPolaridad")).isGreaterThanOrEqualTo(0.84);
    assertThat(metricas.get("exclusionesCorrectas")).isEqualTo("17 de 17");
  }

  @Test
  void todaCitaApareceTextualmenteYLasFrecuenciasCuadran() {
    Preparacion[] preparacion = new Preparacion[1];
    Salida salida = correr(entrada(), preparacion);
    Map<Long, String> textos = new HashMap<>();
    preparacion[0].utiles().forEach(c -> textos.put(c.id(), c.texto()));
    ResultadoAnalisis resultado = salida.resultado();
    assertThat(resultado.temas()).isNotEmpty();
    resultado
        .temas()
        .forEach(
            tema -> {
              assertThat(tema.citas()).isNotEmpty();
              tema.citas()
                  .forEach(
                      cita ->
                          assertThat(textos.get(cita.comentarioId())).contains(cita.fragmento()));
              long etiquetados =
                  salida.etiquetados().stream()
                      .filter(
                          e ->
                              e.etiquetas().stream()
                                  .anyMatch(
                                      x ->
                                          x.descriptor().equals(tema.descriptor())
                                              && x.polaridad().equals(tema.polaridad())))
                      .count();
              assertThat(tema.frecuencia()).isEqualTo((int) etiquetados);
            });
  }

  @Test
  void ningunDatoPersonalLlegaALaSalida() throws Exception {
    Preparacion[] preparacion = new Preparacion[1];
    Salida salida = correr(entrada(), preparacion);
    ObjectMapper json = new ObjectMapper();
    String todo =
        json.writeValueAsString(salida.resultado())
            + json.writeValueAsString(preparacion[0].utiles());
    for (JsonNode c : datos.path("comentarios")) {
      for (JsonNode dato : c.path("pii")) {
        assertThat(todo).doesNotContain(dato.asText());
      }
    }
    assertThat(todo).doesNotContain("Ana María").doesNotContain("Ruiz");
  }

  @Test
  void losIntentosDeInyeccionNoCambianElResultado() throws Exception {
    List<Evaluado> sinInyeccion = new ArrayList<>();
    for (JsonNode c : datos.path("comentarios")) {
      if (!"INSTRUCCION_SOSPECHOSA".equals(c.path("excluir").asText())) {
        sinInyeccion.add(new Evaluado(c.path("id").asLong(), c.path("texto").asText()));
      }
    }
    ResultadoAnalisis con = correr(entrada(), new Preparacion[1]).resultado();
    ResultadoAnalisis sin = correr(sinInyeccion, new Preparacion[1]).resultado();
    ObjectMapper json = new ObjectMapper();
    assertThat(json.writeValueAsString(con.temas()))
        .isEqualTo(json.writeValueAsString(sin.temas()));
    assertThat(con.sentimientoGlobal()).isEqualTo(sin.sentimientoGlobal());
    assertThat(con.nExcluidos()).containsEntry("INSTRUCCION_SOSPECHOSA", 3);
  }

  @Test
  void dosCorridasIgualesDanLoMismoAunqueCambieElOrdenDeEntrada() throws Exception {
    List<Evaluado> invertida = new ArrayList<>(entrada());
    Collections.reverse(invertida);
    ObjectMapper json = new ObjectMapper();
    String una = json.writeValueAsString(correr(entrada(), new Preparacion[1]).resultado());
    String otra = json.writeValueAsString(correr(entrada(), new Preparacion[1]).resultado());
    String alReves = json.writeValueAsString(correr(invertida, new Preparacion[1]).resultado());
    assertThat(una).isEqualTo(otra).isEqualTo(alReves);
  }

  @Test
  void conPocosComentariosLoAdvierteSinInventarTemas() {
    List<Evaluado> pocos =
        List.of(
            new Evaluado(1L, "Explica muy bien los temas de la clase."),
            new Evaluado(2L, "Las clases son aburridas y lentas."));
    ResultadoAnalisis resultado = correr(pocos, new Preparacion[1]).resultado();
    assertThat(resultado.advertencias()).anyMatch(a -> a.tipo().equals("POCOS_COMENTARIOS"));
    assertThat(resultado.temas()).noneMatch(t -> t.recurrente());
    assertThat(resultado.tendencias()).allMatch(t -> t.cambio().equals("SIN_REFERENCIA"));
  }

  @Test
  void dosMilComentariosSeAnalizanEnPocosSegundos() {
    List<Evaluado> muchos = new ArrayList<>();
    List<Evaluado> base = entrada();
    for (int i = 0; i < 2000; i++) {
      Evaluado e = base.get(i % base.size());
      muchos.add(new Evaluado((long) i, e.texto() + " Grupo " + (i % 40) + " semana " + i));
    }
    long inicio = System.nanoTime();
    correr(muchos, new Preparacion[1]);
    long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
    System.out.println("RENDIMIENTO 2000 comentarios en " + milisegundos + " ms");
    assertThat(milisegundos).isLessThan(5000);
  }
}
