package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.service.analisis.ModeloIA.ComentarioAnonimo;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprueba que lo que el modelo cita exista de verdad en un comentario de los que recibió. Se
 * compara el texto normalizado (minúsculas, sin tildes, sin signos de puntuación y con espacios
 * simples), así que un cambio de mayúsculas o de comas no invalida la cita, pero una frase
 * inventada o parafraseada sí.
 */
public class VerificadorCitas {

  /** Un fragmento de una sola palabra encajaría en casi cualquier comentario. */
  static final int PALABRAS_MINIMAS = 2;

  private final Map<Long, String> textosNormalizados;

  /**
   * @param comentariosEnviados los comentarios que se le enviaron al modelo (sin los filtrados)
   */
  public VerificadorCitas(List<ComentarioAnonimo> comentariosEnviados) {
    this.textosNormalizados =
        comentariosEnviados.stream()
            .collect(
                Collectors.toMap(
                    ComentarioAnonimo::id,
                    comentario -> normalizar(comentario.texto()),
                    (primero, segundo) -> primero));
  }

  /**
   * @param idComentario comentario que dice citar el modelo
   * @param fragmento texto citado
   * @return verdadero si el fragmento aparece en ese comentario
   */
  public boolean esVerificable(Long idComentario, String fragmento) {
    if (idComentario == null || fragmento == null) {
      return false;
    }
    String texto = textosNormalizados.get(idComentario);
    String buscado = normalizar(fragmento);
    return texto != null
        && buscado.split(" ").length >= PALABRAS_MINIMAS
        && (" " + texto + " ").contains(" " + buscado + " ");
  }

  static String normalizar(String texto) {
    return Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^\\p{L}\\p{N}\\[\\]]+", " ")
        .strip();
  }
}
