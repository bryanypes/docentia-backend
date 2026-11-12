package com.uniautonoma.docentia.service.analisis.local;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uniautonoma.docentia.service.analisis.ModeloIA;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Comentario;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Cita;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Tema;
import java.util.List;

/**
 * Modelo local por reglas, el de por defecto. No es un modelo generativo: aplica el léxico
 * versionado y propone como hallazgos los temas recurrentes, con citas textuales. Los hallazgos
 * quedan pendientes de validación como los de cualquier modelo.
 */
public class ModeloIALocal implements ModeloIA {

  private static final int CITAS_POR_HALLAZGO = 3;

  private final AnalizadorLocal analizador;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ModeloIALocal(AnalizadorLocal analizador) {
    this.analizador = analizador;
  }

  @Override
  public String tipo() {
    return "REGLAS";
  }

  @Override
  public RespuestaModelo analizar(PeticionAnalisis peticion) {
    List<Comentario> comentarios =
        peticion.comentarios().stream().map(c -> new Comentario(c.id(), c.texto())).toList();
    ResultadoAnalisis resultado = analizador.analizar(comentarios, null).resultado();
    ObjectNode raiz = objectMapper.createObjectNode();
    ArrayNode hallazgos = raiz.putArray("hallazgos");
    resultado
        .aspectos()
        .fortalezas()
        .forEach(t -> agregar(hallazgos, "fortaleza", t, resultado.nComentarios()));
    resultado
        .aspectos()
        .oportunidades()
        .forEach(t -> agregar(hallazgos, "oportunidad_de_mejora", t, resultado.nComentarios()));
    try {
      return new RespuestaModelo(objectMapper.writeValueAsString(raiz), resultado.versionModelo());
    } catch (JsonProcessingException fallo) {
      throw new ModeloIAException("No se pudo armar la respuesta del modelo local", fallo);
    }
  }

  private static void agregar(ArrayNode hallazgos, String tipo, Tema tema, int total) {
    ObjectNode hallazgo = hallazgos.addObject();
    hallazgo.put("tipo", tipo);
    hallazgo.put(
        "texto",
        (tipo.equals("fortaleza")
                ? "Fortaleza mencionada con frecuencia: "
                : "Oportunidad de mejora mencionada con frecuencia: ")
            + tema.nombre()
            + " ("
            + tema.frecuencia()
            + " de "
            + total
            + " comentarios).");
    ArrayNode citas = hallazgo.putArray("citas");
    for (Cita cita : tema.citas().stream().limit(CITAS_POR_HALLAZGO).toList()) {
      citas.addObject().put("comentarioId", cita.comentarioId()).put("fragmento", cita.fragmento());
    }
  }
}
