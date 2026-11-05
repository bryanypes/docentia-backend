package com.uniautonoma.docentia.service.analisis;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.service.analisis.ModeloIA.ComentarioAnonimo;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Comentario;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Etiquetado;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Salida;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal;
import com.uniautonoma.docentia.service.analisis.local.FiltroCalidad;
import com.uniautonoma.docentia.service.analisis.local.FiltroCalidad.Evaluado;
import com.uniautonoma.docentia.service.analisis.local.Lexico;
import com.uniautonoma.docentia.service.analisis.local.MuestreoParaModelo;
import com.uniautonoma.docentia.service.analisis.local.MuestreoParaModelo.Muestra;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis.Preparacion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MuestreoParaModeloTest {

  private static Lexico lexico;
  private static PipelineAnalisis pipeline;
  private static Preparacion preparacion;
  private static Salida salida;
  private static final String INSTRUCCION = "Instrucción fija de prueba";

  @BeforeAll
  static void preparar() {
    lexico = Lexico.cargar("ia/lexico-v1.txt");
    pipeline =
        new PipelineAnalisis(
            new FiltroCalidad(lexico, 3),
            new FiltroPorReglas(new ClassPathResource("filtros/reglas-comentarios.txt")),
            new Anonimizador());
    List<Evaluado> entrada = new ArrayList<>();
    List<String> textos = GeneradorComentariosSinteticos.generar(5000);
    for (int i = 0; i < textos.size(); i++) {
      entrada.add(new Evaluado((long) i + 1, textos.get(i)));
    }
    preparacion = pipeline.preparar(entrada, "Ana María Ruiz");
    salida =
        new AnalizadorLocal(lexico, ValidacionAnalisisTest.PARAMETROS)
            .analizar(preparacion.utiles(), null);
  }

  private static Muestra muestra(List<Comentario> utiles, List<Etiquetado> etiquetados) {
    return new MuestreoParaModelo(150, 300, 16000)
        .elegir(utiles, etiquetados, INSTRUCCION, List.of("D05+4"));
  }

  private static Set<String> criterios(List<Etiquetado> etiquetados, Set<Long> ids) {
    Set<String> criterios = new TreeSet<>();
    etiquetados.stream()
        .filter(e -> ids == null || ids.contains(e.id()))
        .forEach(e -> e.etiquetas().forEach(x -> criterios.add(x.criterio() + x.polaridad())));
    return criterios;
  }

  @Test
  void conCincoMilComentariosSeEnviaComoMaximoElTopeYConTodosLosCriterios() {
    assertThat(preparacion.utiles()).hasSizeGreaterThan(1000);
    Muestra muestra = muestra(preparacion.utiles(), salida.etiquetados());

    assertThat(muestra.enviados()).hasSizeLessThanOrEqualTo(150).hasSizeGreaterThan(100);
    assertThat(muestra.muestreado()).isTrue();
    assertThat(muestra.disponibles()).isEqualTo(preparacion.utiles().size());
    Set<Long> enviados = new java.util.HashSet<>();
    muestra.enviados().forEach(c -> enviados.add(c.id()));
    // Todo criterio y polaridad presentes en el total aparece en la muestra.
    assertThat(criterios(salida.etiquetados(), enviados))
        .containsAll(criterios(salida.etiquetados(), null));
    assertThat(muestra.tokensEstimados()).isLessThanOrEqualTo(16000);
    assertThat(muestra.tokensSinTopes()).isGreaterThan(muestra.tokensEstimados() * 10);
  }

  @Test
  void laMuestraEsReproducibleYNoDependeDelOrdenDeEntrada() {
    Muestra una = muestra(preparacion.utiles(), salida.etiquetados());
    List<Comentario> invertidos = new ArrayList<>(preparacion.utiles());
    Collections.reverse(invertidos);
    List<Etiquetado> etiquetadosInvertidos = new ArrayList<>(salida.etiquetados());
    Collections.reverse(etiquetadosInvertidos);
    Muestra otra = muestra(invertidos, etiquetadosInvertidos);

    assertThat(otra.enviados()).isEqualTo(una.enviados());
  }

  @Test
  void cadaComentarioSeRecortaEnUnaPalabraYSeMarca() {
    String largo = "Explica muy bien los temas de la clase con ejemplos " + "claros ".repeat(80);
    MuestreoParaModelo muestreo = new MuestreoParaModelo(150, 300, 16000);
    Muestra muestra =
        muestreo.elegir(
            List.of(new Comentario(1L, largo)),
            List.of(new Etiquetado(1L, "POSITIVO", List.of())),
            INSTRUCCION,
            List.of());

    ComentarioAnonimo enviado = muestra.enviados().get(0);
    assertThat(enviado.texto()).hasSizeLessThanOrEqualTo(301).endsWith("…");
    assertThat(largo).startsWith(enviado.texto().substring(0, enviado.texto().length() - 1));
    assertThat(enviado.texto().charAt(enviado.texto().length() - 2)).isNotEqualTo(' ');
    assertThat(muestra.recortados()).isEqualTo(1);
  }

  @Test
  void quitaSaludosFirmasYCasiDuplicados() {
    Map<Long, String> textos = new HashMap<>();
    textos.put(1L, "Hola profe, explica muy bien los temas. Gracias.");
    textos.put(2L, "Buenas tardes. Explica muy bien los temas. Saludos.");
    textos.put(3L, "Hola profe, gracias");
    List<Comentario> utiles = new ArrayList<>();
    List<Etiquetado> etiquetados = new ArrayList<>();
    textos.forEach(
        (id, texto) -> {
          utiles.add(new Comentario(id, texto));
          etiquetados.add(new Etiquetado(id, "POSITIVO", List.of()));
        });

    Muestra muestra =
        new MuestreoParaModelo(150, 300, 16000).elegir(utiles, etiquetados, "", List.of());

    assertThat(muestra.enviados()).hasSize(1);
    assertThat(muestra.enviados().get(0).texto())
        .isEqualToIgnoringCase("explica muy bien los temas");
    assertThat(muestra.sinContenido()).isEqualTo(2);
  }

  @Test
  void siNoCabeEnElTopeDeTokensAchicaLaMuestraYNoLaInstruccion() {
    Muestra muestra =
        new MuestreoParaModelo(150, 300, 2000)
            .elegir(preparacion.utiles(), salida.etiquetados(), INSTRUCCION, List.of());

    assertThat(muestra.tokensEstimados()).isLessThanOrEqualTo(2000);
    assertThat(muestra.enviados()).hasSizeLessThan(150).isNotEmpty();
  }
}
