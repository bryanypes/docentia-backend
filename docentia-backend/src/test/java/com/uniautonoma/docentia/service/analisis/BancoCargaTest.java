package com.uniautonoma.docentia.service.analisis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uniautonoma.docentia.config.IaProperties;
import com.uniautonoma.docentia.service.analisis.ModeloIA.PeticionAnalisis;
import com.uniautonoma.docentia.service.analisis.ModeloIA.RespuestaModelo;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Salida;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal;
import com.uniautonoma.docentia.service.analisis.local.FiltroCalidad;
import com.uniautonoma.docentia.service.analisis.local.FiltroCalidad.Evaluado;
import com.uniautonoma.docentia.service.analisis.local.Lexico;
import com.uniautonoma.docentia.service.analisis.local.MuestreoParaModelo;
import com.uniautonoma.docentia.service.analisis.local.MuestreoParaModelo.Muestra;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis.Preparacion;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Banco de carga del análisis: 50, 200, 1.000, 2.000 y 5.000 comentarios sintéticos. Mide el modelo
 * local (tiempo y memoria) y estima los tokens de la línea base (todos los comentarios completos,
 * como hasta la versión anterior) frente a la muestra compacta. No corre en la suite normal:
 *
 * <pre>
 * ./mvnw test -Dtest=BancoCargaTest -Ddocentia.banco=true                    # solo local y estimado
 * ./mvnw test -Dtest=BancoCargaTest -Ddocentia.banco=true -Ddocentia.banco.real=true   # con .env
 * </pre>
 *
 * Con la opción real llama al proveedor para N = 50, 200 y 1.000, antes y después, con un tope de
 * 200.000 tokens y 10 llamadas en total. Solo imprime números: nunca la respuesta ni la clave.
 */
class BancoCargaTest {

  private static final int[] TAMANOS = {50, 200, 1000, 2000, 5000};
  private static final int[] TAMANOS_REALES = {50, 200, 1000};
  private static final long TOPE_TOKENS = 200_000;
  private static final int TOPE_LLAMADAS = 10;
  private static final int SALIDA_LEGADO = 4096;

  private final ObjectMapper mapper = new ObjectMapper();
  private long tokensReales;
  private int llamadasReales;

  @Test
  void medir() throws Exception {
    Assumptions.assumeTrue(Boolean.getBoolean("docentia.banco"), "banco apagado");
    Lexico lexico = Lexico.cargar("ia/lexico-v1.txt");
    PipelineAnalisis pipeline =
        new PipelineAnalisis(
            new FiltroCalidad(lexico, 3),
            new FiltroPorReglas(new ClassPathResource("filtros/reglas-comentarios.txt")),
            new Anonimizador());
    AnalizadorLocal analizador = new AnalizadorLocal(lexico, ValidacionAnalisisTest.PARAMETROS);
    String v2 = leer("prompts/analisis-v2.txt");
    String v3 = InstruccionAnalisis.cargar().texto();
    boolean real = Boolean.getBoolean("docentia.banco.real") && hayProveedor();
    ModeloIAChatCompletions compacto = real ? proveedor(4000) : null;

    List<Map<String, Object>> filas = new ArrayList<>();
    for (int n : TAMANOS) {
      List<String> textos = GeneradorComentariosSinteticos.generar(n);
      List<Evaluado> entrada = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        entrada.add(new Evaluado((long) i + 1, textos.get(i)));
      }
      System.gc();
      Runtime rt = Runtime.getRuntime();
      long antes = rt.totalMemory() - rt.freeMemory();
      long inicio = System.nanoTime();
      Preparacion preparacion = pipeline.preparar(entrada, "Ana María Ruiz");
      Salida salida = analizador.analizar(preparacion.utiles(), null);
      long ms = (System.nanoTime() - inicio) / 1_000_000;
      long memoria = Math.max(0, (rt.totalMemory() - rt.freeMemory()) - antes) / (1024 * 1024);

      List<String> temas =
          salida.resultado().temas().stream()
              .limit(10)
              .map(
                  t ->
                      t.descriptor()
                          + (t.polaridad().equals("POSITIVO") ? "+" : "-")
                          + t.frecuencia())
              .toList();
      Muestra muestra =
          new MuestreoParaModelo(150, 300, 16000)
              .elegir(preparacion.utiles(), salida.etiquetados(), v3, temas);
      String cuerpoLegado = cuerpoLegado(v2, preparacion);

      Map<String, Object> fila = new LinkedHashMap<>();
      fila.put("n", n);
      fila.put("utiles", preparacion.utiles().size());
      fila.put("localMs", ms);
      fila.put("localMb", memoria);
      fila.put("antesEntradaEstimada", EstimadorTokens.estimar(cuerpoLegado));
      fila.put("despuesEnviados", muestra.enviados().size());
      fila.put("despuesEntradaEstimada", muestra.tokensEstimados());
      if (real && contiene(TAMANOS_REALES, n)) {
        fila.put("antesReal", llamarLegado(cuerpoLegado));
        fila.put(
            "despuesReal",
            llamar(compacto, new PeticionAnalisis(v3, muestra.enviados(), temas), muestra));
      }
      filas.add(fila);
      System.out.println("BANCO " + mapper.writeValueAsString(fila));
    }
    System.out.println("BANCO_TOTAL tokensReales=" + tokensReales + " llamadas=" + llamadasReales);
    Path salidaJson = Path.of("target", "banco-ia.json");
    Files.writeString(
        salidaJson,
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(filas),
        StandardCharsets.UTF_8);
  }

  private Map<String, Object> llamar(
      ModeloIAChatCompletions proveedor, PeticionAnalisis peticion, Muestra muestra) {
    if (!cabe(muestra.tokensEstimados() + 4000)) {
      return Map.of("omitido", "tope de gasto");
    }
    long inicio = System.nanoTime();
    RespuestaModelo respuesta = proveedor.analizar(peticion);
    llamadasReales++;
    tokensReales += respuesta.uso().entrada() + respuesta.uso().salida();
    return Map.of(
        "entrada", respuesta.uso().entrada(),
        "salida", respuesta.uso().salida(),
        "cache", respuesta.uso().cache(),
        "estimado", respuesta.uso().estimado(),
        "ms", (System.nanoTime() - inicio) / 1_000_000);
  }

  /**
   * La llamada como era antes: instrucción v2, todos los comentarios completos, citas con texto.
   */
  private Map<String, Object> llamarLegado(String cuerpo) throws Exception {
    if (!cabe(EstimadorTokens.estimar(cuerpo) + SALIDA_LEGADO)) {
      return Map.of("omitido", "tope de gasto");
    }
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofSeconds(10));
    fabrica.setReadTimeout(Duration.ofSeconds(180));
    long inicio = System.nanoTime();
    JsonNode respuesta =
        RestClient.builder()
            .requestFactory(fabrica)
            .baseUrl(System.getenv("DOCENTIA_IA_URL_BASE"))
            .defaultHeaders(h -> h.setBearerAuth(System.getenv("DOCENTIA_IA_CLAVE")))
            .build()
            .post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(cuerpo)
            .retrieve()
            .body(JsonNode.class);
    llamadasReales++;
    JsonNode uso = respuesta.path("usage");
    int entrada = uso.path("prompt_tokens").asInt();
    int salida = uso.path("completion_tokens").asInt();
    tokensReales += entrada + salida;
    return Map.of(
        "entrada",
        entrada,
        "salida",
        salida,
        "cache",
        uso.at("/prompt_tokens_details/cached_tokens").asInt(0),
        "estimado",
        false,
        "ms",
        (System.nanoTime() - inicio) / 1_000_000);
  }

  private boolean cabe(long estimado) {
    boolean cabe = llamadasReales < TOPE_LLAMADAS && tokensReales + estimado <= TOPE_TOKENS;
    if (!cabe) {
      System.out.println("BANCO_TOPE se omite una llamada: el gasto se acercaba al tope");
    }
    return cabe;
  }

  private String cuerpoLegado(String instruccion, Preparacion preparacion) throws Exception {
    ObjectNode cuerpo = mapper.createObjectNode();
    cuerpo.put("model", System.getenv().getOrDefault("DOCENTIA_IA_MODELO", "modelo"));
    if (!"false".equalsIgnoreCase(System.getenv("DOCENTIA_IA_ENVIAR_TEMPERATURA"))) {
      cuerpo.put("temperature", 0);
    }
    cuerpo.put("max_tokens", SALIDA_LEGADO);
    ArrayNode mensajes = cuerpo.putArray("messages");
    mensajes.addObject().put("role", "system").put("content", instruccion);
    ObjectNode entrada = mapper.createObjectNode();
    ArrayNode comentarios = entrada.putArray("comentarios");
    preparacion
        .comoAnonimos()
        .forEach(c -> comentarios.addObject().put("id", c.id()).put("texto", c.texto()));
    mensajes.addObject().put("role", "user").put("content", mapper.writeValueAsString(entrada));
    return mapper.writeValueAsString(cuerpo);
  }

  private static boolean hayProveedor() {
    return System.getenv("DOCENTIA_IA_URL_BASE") != null
        && System.getenv("DOCENTIA_IA_MODELO") != null
        && System.getenv("DOCENTIA_IA_CLAVE") != null;
  }

  private static ModeloIAChatCompletions proveedor(int salida) {
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofSeconds(10));
    fabrica.setReadTimeout(Duration.ofSeconds(180));
    return new ModeloIAChatCompletions(
        RestClient.builder().requestFactory(fabrica),
        new IaProperties(
            true,
            "chat-completions",
            System.getenv("DOCENTIA_IA_URL_BASE"),
            System.getenv("DOCENTIA_IA_MODELO"),
            180,
            System.getenv("DOCENTIA_IA_CLAVE"),
            10,
            new IaProperties.Worker(false, 5000),
            !"false".equalsIgnoreCase(System.getenv("DOCENTIA_IA_ENVIAR_TEMPERATURA"))),
        salida);
  }

  private static boolean contiene(int[] valores, int n) {
    for (int v : valores) {
      if (v == n) {
        return true;
      }
    }
    return false;
  }

  private static String leer(String recurso) throws Exception {
    try (InputStream entrada = new ClassPathResource(recurso).getInputStream()) {
      return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Una llamada compacta con N = 50 para revisar si la respuesta llega completa. Solo números. */
  @Test
  void diagnosticarSalida() throws Exception {
    Assumptions.assumeTrue(Boolean.getBoolean("docentia.banco.diagnostico") && hayProveedor());
    Lexico lexico = Lexico.cargar("ia/lexico-v1.txt");
    PipelineAnalisis pipeline =
        new PipelineAnalisis(
            new FiltroCalidad(lexico, 3),
            new FiltroPorReglas(new ClassPathResource("filtros/reglas-comentarios.txt")),
            new Anonimizador());
    List<String> textos = GeneradorComentariosSinteticos.generar(50);
    List<Evaluado> entrada = new ArrayList<>();
    for (int i = 0; i < textos.size(); i++) {
      entrada.add(new Evaluado((long) i + 1, textos.get(i)));
    }
    Preparacion preparacion = pipeline.preparar(entrada, "Ana María Ruiz");
    Salida salida =
        new AnalizadorLocal(lexico, ValidacionAnalisisTest.PARAMETROS)
            .analizar(preparacion.utiles(), null);
    String v3 = InstruccionAnalisis.cargar().texto();
    Muestra muestra =
        new MuestreoParaModelo(150, 300, 16000)
            .elegir(preparacion.utiles(), salida.etiquetados(), v3, List.of());
    int salidaMaxima = Integer.getInteger("docentia.banco.salida", 4000);
    ModeloIAChatCompletions proveedor = proveedor(salidaMaxima);
    String cuerpo = proveedor.cuerpo(new PeticionAnalisis(v3, muestra.enviados(), List.of()));
    JsonNode respuesta =
        RestClient.builder()
            .baseUrl(System.getenv("DOCENTIA_IA_URL_BASE"))
            .defaultHeaders(h -> h.setBearerAuth(System.getenv("DOCENTIA_IA_CLAVE")))
            .build()
            .post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(cuerpo)
            .retrieve()
            .body(JsonNode.class);
    String contenido = respuesta.at("/choices/0/message/content").asText("");
    String valido;
    int hallazgos = -1;
    try {
      hallazgos = new LectorRespuestaAnalisis().leer(contenido).size();
      valido = "si";
    } catch (RuntimeException e) {
      valido = "no";
    }
    System.out.println(
        "DIAGNOSTICO finish="
            + respuesta.at("/choices/0/finish_reason").asText()
            + " caracteres="
            + contenido.length()
            + " jsonValido="
            + valido
            + " hallazgos="
            + hallazgos
            + " usage="
            + respuesta.path("usage").path("completion_tokens").asInt()
            + "/"
            + respuesta.path("usage").path("prompt_tokens").asInt()
            + " empiezaConLlave="
            + contenido.strip().startsWith("{"));
  }

  /**
   * Una sola llamada con la muestra de 150 comentarios (de 1.000 sintéticos) y la salida de 4.000,
   * para ver la respuesta tal como la escribe el modelo. Solo con
   * -Ddocentia.banco.verificacion=true.
   */
  @Test
  void verificarRespuestaCompleta() throws Exception {
    Assumptions.assumeTrue(Boolean.getBoolean("docentia.banco.verificacion") && hayProveedor());
    Lexico lexico = Lexico.cargar("ia/lexico-v1.txt");
    PipelineAnalisis pipeline =
        new PipelineAnalisis(
            new FiltroCalidad(lexico, 3),
            new FiltroPorReglas(new ClassPathResource("filtros/reglas-comentarios.txt")),
            new Anonimizador());
    List<String> textos = GeneradorComentariosSinteticos.generar(1000);
    List<Evaluado> entrada = new ArrayList<>();
    for (int i = 0; i < textos.size(); i++) {
      entrada.add(new Evaluado((long) i + 1, textos.get(i)));
    }
    Preparacion preparacion = pipeline.preparar(entrada, "Ana María Ruiz");
    Salida salida =
        new AnalizadorLocal(lexico, ValidacionAnalisisTest.PARAMETROS)
            .analizar(preparacion.utiles(), null);
    List<String> temas =
        salida.resultado().temas().stream()
            .limit(10)
            .map(
                t ->
                    t.descriptor()
                        + (t.polaridad().equals("POSITIVO") ? "+" : "-")
                        + t.frecuencia())
            .toList();
    String v3 = InstruccionAnalisis.cargar().texto();
    Muestra muestra =
        new MuestreoParaModelo(150, 300, 16000)
            .elegir(preparacion.utiles(), salida.etiquetados(), v3, temas);
    String cuerpo = proveedor(4000).cuerpo(new PeticionAnalisis(v3, muestra.enviados(), temas));
    long inicio = System.nanoTime();
    JsonNode respuesta =
        RestClient.builder()
            .baseUrl(System.getenv("DOCENTIA_IA_URL_BASE"))
            .defaultHeaders(h -> h.setBearerAuth(System.getenv("DOCENTIA_IA_CLAVE")))
            .build()
            .post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(cuerpo)
            .retrieve()
            .body(JsonNode.class);
    long ms = (System.nanoTime() - inicio) / 1_000_000;
    String contenido = respuesta.at("/choices/0/message/content").asText("");
    JsonNode uso = respuesta.path("usage");
    System.out.println(
        "VERIFICACION enviados="
            + muestra.enviados().size()
            + " recortados="
            + muestra.recortados()
            + " finish="
            + respuesta.at("/choices/0/finish_reason").asText()
            + " entrada="
            + uso.path("prompt_tokens").asInt()
            + " salida="
            + uso.path("completion_tokens").asInt()
            + " cache="
            + uso.at("/prompt_tokens_details/cached_tokens").asInt(0)
            + " ms="
            + ms
            + " caracteresRespuesta="
            + contenido.length());
    java.util.Set<Long> ids = new java.util.HashSet<>();
    muestra.enviados().forEach(c -> ids.add(c.id()));
    try {
      LectorRespuestaAnalisis lector = new LectorRespuestaAnalisis();
      for (LectorRespuestaAnalisis.HallazgoPropuesto h : lector.leer(contenido)) {
        long validas = h.citas().stream().filter(c -> ids.contains(c.comentarioId())).count();
        System.out.println(
            "VERIFICACION_HALLAZGO "
                + h.tipo()
                + " | "
                + h.texto()
                + " | citas="
                + h.citas().size()
                + " validas="
                + validas);
      }
      System.out.println("VERIFICACION_RESUMEN " + lector.resumen(contenido));
    } catch (RuntimeException invalida) {
      System.out.println("VERIFICACION_INVALIDA " + invalida.getMessage());
    }
  }
}
