package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Promedio ponderado de un docente a partir de las calificaciones registradas en un periodo. */
@Service
@Transactional(readOnly = true)
public class PromedioService {

  private final CalificacionRepository calificacionRepository;
  private final PonderacionService ponderacionService;
  private final CalculadoraPromedioPonderado calculadora;

  public PromedioService(
      CalificacionRepository calificacionRepository,
      PonderacionService ponderacionService,
      CalculadoraPromedioPonderado calculadora) {
    this.calificacionRepository = calificacionRepository;
    this.ponderacionService = ponderacionService;
    this.calculadora = calculadora;
  }

  /**
   * Calcula el promedio ponderado de un docente en un periodo, con los pesos de ese periodo.
   *
   * @param idDocente identificador del docente
   * @param periodo periodo evaluado
   * @return promedio completo, o incompleto si alguna fuente no ha calificado
   */
  public PromedioPonderadoResponse calcularPromedioDocente(
      Long idDocente, PeriodoAcademico periodo) {
    Map<FuenteEvaluacion, BigDecimal> notasPorFuente = new EnumMap<>(FuenteEvaluacion.class);
    for (Calificacion calificacion :
        calificacionRepository.findByDocenteIdUsuarioAndPeriodoIdPeriodo(
            idDocente, periodo.getIdPeriodo())) {
      notasPorFuente.put(calificacion.getFuente(), calificacion.getNotas().promedio());
    }
    return calculadora.calcularPromedio(notasPorFuente, ponderacionService.buscarPesos(periodo));
  }
}
