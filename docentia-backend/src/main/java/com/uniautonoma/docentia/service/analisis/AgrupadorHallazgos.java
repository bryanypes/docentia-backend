package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.CitaPropuesta;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.HallazgoPropuesto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deja los hallazgos agrupados aunque el modelo no lo haga: un solo hallazgo por tipo y criterio
 * (los repetidos se funden) y cada comentario en un solo hallazgo, el más respaldado. Un hallazgo
 * que se queda sin comentarios propios desaparece: lo que decía ya está en otro.
 */
public final class AgrupadorHallazgos {

  private AgrupadorHallazgos() {}

  public static List<HallazgoPropuesto> agrupar(List<HallazgoPropuesto> propuestos) {
    // 1. Mismo tipo y criterio: un solo hallazgo, con los textos y las citas de todos. Se conserva
    // el orden en que los propuso el modelo; sin criterio, cada uno es su propio grupo.
    Map<Object, HallazgoPropuesto> porGrupo = new LinkedHashMap<>();
    for (HallazgoPropuesto propuesto : propuestos) {
      Object grupo =
          propuesto.criterio() == null
              ? new Object()
              : propuesto.tipo() + "|" + propuesto.criterio();
      porGrupo.merge(grupo, propuesto, AgrupadorHallazgos::fundir);
    }
    List<HallazgoPropuesto> agrupados = new ArrayList<>(porGrupo.values());

    // 2. Cada comentario en un solo hallazgo: primero se sirven los más respaldados.
    List<HallazgoPropuesto> porRespaldo = new ArrayList<>(agrupados);
    porRespaldo.sort(Comparator.comparingInt((HallazgoPropuesto h) -> h.citas().size()).reversed());
    Set<String> usados = new HashSet<>();
    Map<HallazgoPropuesto, List<CitaPropuesta>> propias = new IdentityHashMap<>();
    for (HallazgoPropuesto hallazgo : porRespaldo) {
      List<CitaPropuesta> citas = new ArrayList<>();
      for (CitaPropuesta cita : hallazgo.citas()) {
        if (cita.comentarioId() == null || usados.add(clave(cita))) {
          citas.add(cita);
        }
      }
      propias.put(hallazgo, citas);
    }

    List<HallazgoPropuesto> resultado = new ArrayList<>();
    for (HallazgoPropuesto hallazgo : agrupados) {
      List<CitaPropuesta> citas = propias.get(hallazgo);
      if (citas.isEmpty() && !hallazgo.citas().isEmpty()) {
        continue;
      }
      resultado.add(
          new HallazgoPropuesto(hallazgo.tipo(), hallazgo.texto(), citas, hallazgo.criterio()));
    }
    return resultado;
  }

  /**
   * El comentario citado completo (formato compacto, sin fragmento) cuenta una sola vez; si se
   * citan frases distintas de un mismo comentario, cada frase es una cita aparte.
   */
  private static String clave(CitaPropuesta cita) {
    String fragmento = cita.fragmento() == null ? "" : cita.fragmento().strip().toLowerCase();
    return cita.comentarioId() + "|" + fragmento;
  }

  private static HallazgoPropuesto fundir(HallazgoPropuesto uno, HallazgoPropuesto otro) {
    List<CitaPropuesta> citas = new ArrayList<>(uno.citas());
    Set<String> claves = new HashSet<>();
    uno.citas().forEach(c -> claves.add(clave(c)));
    otro.citas().stream()
        .filter(c -> c.comentarioId() == null || !claves.contains(clave(c)))
        .forEach(citas::add);
    String texto = uno.texto().strip();
    String extra = otro.texto().strip();
    if (!texto.equalsIgnoreCase(extra)) {
      texto = (texto.endsWith(".") ? texto : texto + ".") + " " + extra;
    }
    if (texto.length() > LectorRespuestaAnalisis.LONGITUD_MAXIMA) {
      texto = texto.substring(0, LectorRespuestaAnalisis.LONGITUD_MAXIMA);
    }
    return new HallazgoPropuesto(uno.tipo(), texto, citas, uno.criterio());
  }
}
