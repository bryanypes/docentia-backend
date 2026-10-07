package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.Dimension;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.repository.CalificacionRepository;
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
   * Reúne las calificaciones de un docente en un periodo y los pesos de ese periodo.
   *
   * @param idDocente identificador del docente
   * @param periodo periodo evaluado
   * @return notas por fuente registradas hasta ahora y pesos con que se ponderan
   */
  public EvaluacionPeriodo cargarEvaluacion(Long idDocente, PeriodoAcademico periodo) {
    Map<FuenteEvaluacion, NotasAspectos> notasPorFuente = new EnumMap<>(FuenteEvaluacion.class);
    for (Calificacion calificacion :
        calificacionRepository.findByDocenteIdUsuarioAndPeriodoIdPeriodo(
            idDocente, periodo.getIdPeriodo())) {
      notasPorFuente.put(calificacion.getFuente(), calificacion.getNotas());
    }
    return new EvaluacionPeriodo(notasPorFuente, ponderacionService.buscarPesos(periodo));
  }

  /**
   * Calcula el promedio ponderado general de una evaluación.
   *
   * @param evaluacion calificaciones y pesos del periodo
   * @return promedio completo, o incompleto si alguna fuente no ha calificado
   */
  public PromedioPonderadoResponse calcularPromedio(EvaluacionPeriodo evaluacion) {
    return calculadora.calcularPromedio(evaluacion.notasDeFuentes(), evaluacion.pesos());
  }

  /**
   * Calcula el resultado ponderado de una sola dimensión.
   *
   * @param evaluacion calificaciones y pesos del periodo
   * @param dimension dimensión consultada
   * @return resultado de la dimensión, o incompleto si alguna fuente no ha calificado
   */
  public PromedioPonderadoResponse calcularResultadoDimension(
      EvaluacionPeriodo evaluacion, Dimension dimension) {
    return calculadora.calcularPromedio(evaluacion.notasDeDimension(dimension), evaluacion.pesos());
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
    return calcularPromedio(cargarEvaluacion(idDocente, periodo));
  }
}
