package com.uniautonoma.docentia.service.analisis.local;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Comentario;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal.ParametrosAnalisis;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Léxico v2: jerga colombiana y juvenil, abreviaturas de chat, risas y emojis. Los comentarios son
 * del estilo de los que escriben los estudiantes.
 */
class LexicoJergaTest {

  private static final ParametrosAnalisis PARAMETROS = new ParametrosAnalisis(3, 0.2, 5, 10, 5);
  private static final AnalizadorLocal V1 =
      new AnalizadorLocal(Lexico.cargar("ia/lexico-v1.txt"), PARAMETROS);
  private static final AnalizadorLocal V2 =
      new AnalizadorLocal(Lexico.cargar("ia/lexico-v2.txt"), PARAMETROS);

  private static String sentimiento(AnalizadorLocal analizador, String texto) {
    return analizador
        .analizar(List.of(new Comentario(1L, texto)), null)
        .etiquetados()
        .get(0)
        .sentimiento();
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "Melo, la clase es brutal | POSITIVO",
        "Es un crack, 10 de 10 👏🏽 | POSITIVO",
        "Que chimba de clase, se rifa explicando | POSITIVO",
        "La profe es lo máximo ❤️ | POSITIVO",
        "bkno el profe 🔥🔥 | POSITIVO",
        "La clase es maluca, me duermo 😴 | NEGATIVO",
        "Que pereza esa clase 🙄 | NEGATIVO",
        "Es jarta y la tiene montada a uno | NEGATIVO",
        "Una porquería de metodología | NEGATIVO",
        "No sirve pa nada esa explicación 👎 | NEGATIVO",
      })
  void elV2EntiendeLaJergaYLosEmojis(String comentario, String esperado) {
    assertThat(sentimiento(V2, comentario)).isEqualTo(esperado);
  }

  @Test
  void elV1NoLosEntendia() {
    assertThat(sentimiento(V1, "Melo, la clase es brutal")).isEqualTo("NEUTRO");
    assertThat(sentimiento(V1, "La clase es maluca, me duermo 😴")).isEqualTo("NEUTRO");
  }

  @Test
  void lasRisasYLasAbreviaturasNoCambianElSentido() {
    assertThat(sentimiento(V2, "explica bn jajaja")).isEqualTo("POSITIVO");
    assertThat(sentimiento(V2, "xq no explica bien")).isEqualTo("NEGATIVO");
    assertThat(sentimiento(V2, "jajaja xd")).isEqualTo("NEUTRO");
  }

  @Test
  void laNegacionSigueInvirtiendoLaJerga() {
    assertThat(sentimiento(V2, "no es nada melo")).isEqualTo("NEGATIVO");
  }

  @Test
  void elV2DeclaraSuVersion() {
    assertThat(Lexico.cargar("ia/lexico-v2.txt").version()).isEqualTo("lexico-2026.11-v2");
  }
}
