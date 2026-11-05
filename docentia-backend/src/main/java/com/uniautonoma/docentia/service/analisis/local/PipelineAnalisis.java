package com.uniautonoma.docentia.service.analisis.local;

import com.uniautonoma.docentia.service.analisis.Anonimizador;
import com.uniautonoma.docentia.service.analisis.FiltroComentarios;
import com.uniautonoma.docentia.service.analisis.FiltroComentarios.Exclusion;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ComentarioAnonimo;
import com.uniautonoma.docentia.service.analisis.VerificadorCitas;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Comentario;
import com.uniautonoma.docentia.service.analisis.local.FiltroCalidad.Evaluado;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Advertencia;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Aspectos;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Tema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Ingesta → normalización → filtrado → anonimización → análisis → verificación de citas. Lo usan el
 * flujo asíncrono y la validación, para que se pruebe exactamente lo mismo que corre.
 */
public class PipelineAnalisis {

  private static final Set<String> GRAVES = Set.of("ACOSO", "AMENAZA", "CONDUCTA_GRAVE");

  /** Comentario excluido por el filtro de contenido, para registrarlo como hasta ahora (E2). */
  public record FiltradoContenido(Long id, Exclusion exclusion) {}

  public record Preparacion(
      List<Comentario> utiles,
      Map<String, Integer> excluidos,
      List<FiltradoContenido> filtrados,
      int truncados) {

    public List<ComentarioAnonimo> comoAnonimos() {
      return utiles.stream().map(c -> new ComentarioAnonimo(c.id(), c.texto())).toList();
    }
  }

  private final FiltroCalidad calidad;
  private final FiltroComentarios contenido;
  private final Anonimizador anonimizador;

  public PipelineAnalisis(
      FiltroCalidad calidad, FiltroComentarios contenido, Anonimizador anonimizador) {
    this.calidad = calidad;
    this.contenido = contenido;
    this.anonimizador = anonimizador;
  }

  public Preparacion preparar(List<Evaluado> entrada, String nombreDocente) {
    // Se ordena antes de filtrar: qué copia de un duplicado se conserva no depende del orden de
    // llegada.
    List<Evaluado> ordenada = new ArrayList<>(entrada);
    ordenada.sort(
        java.util.Comparator.comparing((Evaluado c) -> hash(NormalizadorTexto.limpiar(c.texto())))
            .thenComparing(Evaluado::id));
    FiltroCalidad.Resultado filtrado = calidad.filtrar(ordenada);
    Map<String, Integer> excluidos = new java.util.TreeMap<>(filtrado.excluidos());
    List<FiltradoContenido> filtrados = new ArrayList<>();
    List<Comentario> utiles = new ArrayList<>();
    for (Evaluado comentario : filtrado.utiles()) {
      Optional<Exclusion> exclusion = contenido.evaluar(comentario.texto());
      if (exclusion.isPresent()) {
        excluidos.merge(exclusion.get().categoria(), 1, Integer::sum);
        filtrados.add(new FiltradoContenido(comentario.id(), exclusion.get()));
      } else {
        utiles.add(
            new Comentario(
                comentario.id(), anonimizador.anonimizar(comentario.texto(), nombreDocente)));
      }
    }
    // Orden por hash del texto: no depende del orden de inserción ni de fechas.
    utiles.sort(
        java.util.Comparator.comparing((Comentario c) -> hash(c.texto()))
            .thenComparing(Comentario::id));
    return new Preparacion(List.copyOf(utiles), excluidos, filtrados, filtrado.truncados());
  }

  /** Completa el resultado con lo excluido y descarta toda cita que no exista en la entrada. */
  public ResultadoAnalisis completar(ResultadoAnalisis resultado, Preparacion preparacion) {
    VerificadorCitas verificador = new VerificadorCitas(preparacion.comoAnonimos());
    List<Tema> temas = resultado.temas().stream().map(t -> verificadas(t, verificador)).toList();
    Aspectos aspectos =
        new Aspectos(
            resultado.aspectos().fortalezas().stream()
                .map(t -> verificadas(t, verificador))
                .toList(),
            resultado.aspectos().oportunidades().stream()
                .map(t -> verificadas(t, verificador))
                .toList());
    List<Advertencia> advertencias = new ArrayList<>(resultado.advertencias());
    int ofensivos = 0;
    for (Map.Entry<String, Integer> excluido : preparacion.excluidos().entrySet()) {
      String motivo = excluido.getKey();
      if (GRAVES.contains(motivo)) {
        advertencias.add(
            new Advertencia(
                "SENSIBLE_" + motivo,
                excluido.getValue(),
                "Hay comentarios con posibles situaciones graves. No se resumen ni se citan;"
                    + " revísalos en los comentarios excluidos."));
      } else if (motivo.equals("INSTRUCCION_SOSPECHOSA")) {
        advertencias.add(
            new Advertencia(
                motivo,
                excluido.getValue(),
                "Hay comentarios que intentan dar órdenes al modelo; se excluyeron sin analizarlos."));
      } else if (!Set.of("VACIO", "SIN_TEXTO", "MUY_CORTO", "DUPLICADO").contains(motivo)) {
        ofensivos += excluido.getValue();
      }
    }
    if (ofensivos > 0) {
      advertencias.add(
          new Advertencia(
              "LENGUAJE_OFENSIVO",
              ofensivos,
              "Hay comentarios ofensivos o sobre la persona; no se analizan ni se citan."));
    }
    if (preparacion.truncados() > 0) {
      advertencias.add(
          new Advertencia(
              "TRUNCADO",
              preparacion.truncados(),
              "Algunos comentarios superaban 1.000 caracteres y se recortaron."));
    }
    return new ResultadoAnalisis(
        resultado.tipoModelo(),
        resultado.versionModelo(),
        resultado.versionLexicoOPrompt(),
        resultado.nComentarios(),
        new java.util.TreeMap<>(preparacion.excluidos()),
        resultado.sentimientoGlobal(),
        temas,
        aspectos,
        resultado.tendencias(),
        advertencias,
        resultado.criterios(),
        resultado.parametros());
  }

  private static Tema verificadas(Tema tema, VerificadorCitas verificador) {
    return new Tema(
        tema.criterio(),
        tema.descriptor(),
        tema.nombre(),
        tema.polaridad(),
        tema.frecuencia(),
        tema.porcentaje(),
        tema.intensidad(),
        tema.relevancia(),
        tema.recurrente(),
        tema.citas().stream()
            .filter(c -> verificador.esVerificable(c.comentarioId(), c.fragmento()))
            .toList());
  }

  /** SHA-256 del texto en hexadecimal. */
  public static String hash(String texto) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException fallo) {
      throw new IllegalStateException(fallo);
    }
  }
}
