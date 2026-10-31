package com.uniautonoma.docentia.service.analisis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uniautonoma.docentia.service.analisis.FiltroComentarios.Exclusion;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

/** E2: las reglas del archivo real contra comentarios que deben y no deben filtrarse. */
class FiltroPorReglasTest {

  private final FiltroPorReglas filtro =
      new FiltroPorReglas(new ClassPathResource("filtros/reglas-comentarios.txt"));

  @Test
  void excluyeInsultosYComentariosSobreLaPersonaConSuCategoria() {
    Map<String, String> esperados =
        Map.ofEntries(
            Map.entry("Ese profesor es un idiota.", "INSULTO"),
            Map.entry("ES UN INÚTIL, no sirve para nada", "INSULTO"),
            Map.entry("Es un profesor muy feo y se viste mal.", "APARIENCIA"),
            Map.entry("La profe está muy gorda", "APARIENCIA"),
            Map.entry("Debería jubilarse ya", "EDAD"),
            Map.entry("El profesor ya está muy viejo para enseñar", "EDAD"),
            Map.entry("No le creo nada por ser mujer", "GENERO"),
            Map.entry("La profesora es una histérica", "GENERO"),
            Map.entry("Seguro tiene problemas con su esposa", "VIDA_PRIVADA"),
            Map.entry("Llega borracho a veces", "VIDA_PRIVADA"));

    esperados.forEach(
        (comentario, categoria) ->
            assertThat(filtro.evaluar(comentario))
                .as(comentario)
                .get()
                .extracting(Exclusion::categoria)
                .isEqualTo(categoria));
  }

  @Test
  void unaCriticaDuraPeroLegitimaALaPracticaDocenteNoSeFiltra() {
    String[] legitimos = {
      "La metodología es pésima: lee las diapositivas y no explica nada.",
      "Es el peor profesor que he tenido, no prepara las clases.",
      "Sus clases son aburridas y desorganizadas.",
      "El material del curso es viejo y desactualizado.",
      "Califica injustamente y nunca explica los criterios.",
      "El código de ejemplo es feo y difícil de seguir.",
      "Nos trata como su familia y es amante de la programación.",
      "Las notas se demoran tres semanas, es inútil esperar retroalimentación."
    };

    for (String comentario : legitimos) {
      assertThat(filtro.evaluar(comentario)).as(comentario).isEmpty();
    }
  }

  @Test
  void normalizaMayusculasTildesYEspacios() {
    assertThat(filtro.evaluar("ese   PROFESOR   es un   IDIÓTA")).isPresent();
    assertThat(FiltroPorReglas.normalizar("  Está   MUY viejo ")).isEqualTo("esta muy viejo");
  }

  @Test
  void ignoraComentariosDelArchivoYRechazaReglasMalEscritas() {
    FiltroPorReglas propio =
        new FiltroPorReglas(recurso("# comentario\n\nINSULTO | \\btonto\\b\n"));
    assertThat(propio.cantidadDeReglas()).isEqualTo(1);
    assertThat(propio.evaluar("qué tonto")).contains(new Exclusion("INSULTO", "\\btonto\\b"));

    assertThatThrownBy(() -> new FiltroPorReglas(recurso("sin separador\n")))
        .hasMessageContaining("línea 1");
    assertThatThrownBy(() -> new FiltroPorReglas(recurso("INSULTO | (sin cerrar\n")))
        .hasMessageContaining("Expresión inválida en la línea 1");
  }

  private ByteArrayResource recurso(String contenido) {
    return new ByteArrayResource(contenido.getBytes(StandardCharsets.UTF_8));
  }
}
