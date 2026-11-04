package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.entity.EvaluacionFormulario;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.EvaluacionFormularioRepository;
import com.uniautonoma.docentia.service.CalificacionService;
import com.uniautonoma.docentia.service.formulario.CatalogoInstrumento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Coevaluaciones y autoevaluaciones de prueba enviadas por el mismo camino que en la aplicación: un
 * formulario con sus respuestas y la nota de la fuente sacada de ellas. Así Coevaluaciones, el
 * expediente y el proceso cuentan lo mismo, en vez de una nota sin formulario detrás.
 */
@Component
@Profile("dev")
class SemillaFormularios {

  private static final int MINIMO = 1;
  private static final int MAXIMO = 5;

  private final EvaluacionFormularioRepository formularioRepository;
  private final CatalogoInstrumento catalogo;
  private final CalificacionService calificacionService;
  private final Clock reloj;

  SemillaFormularios(
      EvaluacionFormularioRepository formularioRepository,
      CatalogoInstrumento catalogo,
      CalificacionService calificacionService,
      Clock reloj) {
    this.formularioRepository = formularioRepository;
    this.catalogo = catalogo;
    this.calificacionService = calificacionService;
    this.reloj = reloj;
  }

  /**
   * Envía el formulario con respuestas enteras cuya media queda lo más cerca posible de la nota
   * pedida (con seis ítems, 4.3 queda en 4.33).
   */
  void enviar(
      Usuario docente, PeriodoAcademico periodo, TipoFormulario tipo, Usuario evaluador, String nota) {
    List<Long> descriptores =
        catalogo.de(tipo).stream()
            .flatMap(criterio -> criterio.descriptores().stream())
            .map(CatalogoInstrumento.Descriptor::id)
            .toList();
    Map<Long, Short> respuestas = respuestasCon(descriptores, new BigDecimal(nota));
    EvaluacionFormulario formulario =
        new EvaluacionFormulario(periodo, docente, evaluador, tipo, CatalogoInstrumento.VERSION);
    formulario.reemplazar(respuestas, Map.of(), null);
    BigDecimal media =
        BigDecimal.valueOf(respuestas.values().stream().mapToInt(Short::intValue).sum())
            .divide(BigDecimal.valueOf(respuestas.size()), 4, RoundingMode.HALF_UP);
    formulario.enviar(media, LocalDateTime.now(reloj));
    formularioRepository.saveAndFlush(formulario);
    calificacionService.registrarDesdeFormulario(
        docente, periodo, tipo.getFuente(), media, evaluador);
  }

  private static Map<Long, Short> respuestasCon(List<Long> descriptores, BigDecimal nota) {
    int n = descriptores.size();
    int suma =
        nota.multiply(BigDecimal.valueOf(n)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    suma = Math.max(MINIMO * n, Math.min(MAXIMO * n, suma));
    int base = suma / n;
    int extra = suma - base * n;
    Map<Long, Short> respuestas = new LinkedHashMap<>();
    for (int i = 0; i < n; i++) {
      respuestas.put(descriptores.get(i), (short) (i < extra ? base + 1 : base));
    }
    return respuestas;
  }
}
