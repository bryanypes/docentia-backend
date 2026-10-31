package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.Dimension;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.FuentesPendientesConverter;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.PesosFuentes;
import com.uniautonoma.docentia.entity.Ponderacion;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.DocentePeriodo;
import com.uniautonoma.docentia.repository.PonderacionRepository;
import com.uniautonoma.docentia.repository.PromedioDocenteRepository;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promedio ponderado de un docente. Se calcula y se guarda cada vez que cambia una calificación o
 * la ponderación del periodo; las consultas leen el valor guardado.
 */
@Service
@Transactional(readOnly = true)
public class PromedioService {

  private static final FuentesPendientesConverter CONVERTIDOR_FUENTES =
      new FuentesPendientesConverter();

  private final CalificacionRepository calificacionRepository;
  private final PonderacionRepository ponderacionRepository;
  private final PromedioDocenteRepository promedioRepository;
  private final CalculadoraPromedioPonderado calculadora;

  public PromedioService(
      CalificacionRepository calificacionRepository,
      PonderacionRepository ponderacionRepository,
      PromedioDocenteRepository promedioRepository,
      CalculadoraPromedioPonderado calculadora) {
    this.calificacionRepository = calificacionRepository;
    this.ponderacionRepository = ponderacionRepository;
    this.promedioRepository = promedioRepository;
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
    return cargarEvaluacion(idDocente, periodo.getIdPeriodo());
  }

  /**
   * Consulta el promedio guardado de un docente en un periodo, sin recalcularlo.
   *
   * @param idDocente identificador del docente
   * @param periodo periodo evaluado
   * @return el promedio guardado; si el docente no tiene calificaciones en el periodo, un promedio
   *     incompleto con las tres fuentes pendientes
   */
  public PromedioPonderadoResponse obtenerPromedio(Long idDocente, PeriodoAcademico periodo) {
    return obtenerPromedio(idDocente, periodo.getIdPeriodo());
  }

  /**
   * Consulta el promedio guardado de un docente en un periodo, sin recalcularlo.
   *
   * @param idDocente identificador del docente
   * @param idPeriodo identificador del periodo
   * @return el promedio guardado, o incompleto con las tres fuentes si no tiene calificaciones
   */
  public PromedioPonderadoResponse obtenerPromedio(Long idDocente, Long idPeriodo) {
    return promedioRepository
        .leerPromedio(idDocente, idPeriodo)
        .orElseGet(
            () -> PromedioPonderadoResponse.incompleto(Arrays.asList(FuenteEvaluacion.values())));
  }

  /**
   * Calcula el resultado ponderado de una sola dimensión. No se guarda: se deriva de las notas en
   * cada consulta.
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
   * Recalcula y guarda el promedio de un docente en un periodo. Se llama después de registrar o
   * corregir cualquiera de sus calificaciones.
   *
   * @param idDocente identificador del docente
   * @param idPeriodo identificador del periodo
   * @return el promedio guardado
   */
  @Transactional
  public PromedioPonderadoResponse recalcular(Long idDocente, Long idPeriodo) {
    EvaluacionPeriodo evaluacion = cargarEvaluacion(idDocente, idPeriodo);
    PromedioPonderadoResponse resultado =
        calculadora.calcularPromedio(evaluacion.notasDeFuentes(), evaluacion.pesos());
    promedioRepository.guardar(
        idDocente,
        idPeriodo,
        resultado.valor(),
        resultado.estado().name(),
        CONVERTIDOR_FUENTES.convertToDatabaseColumn(resultado.fuentesPendientes()));
    return resultado;
  }

  /**
   * Recalcula los promedios de todos los docentes calificados en un periodo. Se llama cuando
   * cambian las ponderaciones de ese periodo.
   *
   * @param idPeriodo identificador del periodo
   */
  @Transactional
  public void recalcularPeriodo(Long idPeriodo) {
    for (Long idDocente : calificacionRepository.buscarDocentesCalificados(idPeriodo)) {
      recalcular(idDocente, idPeriodo);
    }
  }

  /**
   * Calcula los promedios que faltan: calificaciones registradas antes de que existiera la tabla de
   * promedios.
   *
   * @return cuántos promedios se calcularon
   */
  @Transactional
  public int recalcularPendientes() {
    int calculados = 0;
    for (DocentePeriodo pendiente : calificacionRepository.buscarSinPromedio()) {
      recalcular(pendiente.idDocente(), pendiente.idPeriodo());
      calculados++;
    }
    return calculados;
  }

  private EvaluacionPeriodo cargarEvaluacion(Long idDocente, Long idPeriodo) {
    Map<FuenteEvaluacion, NotasAspectos> notasPorFuente = new EnumMap<>(FuenteEvaluacion.class);
    for (Calificacion calificacion :
        calificacionRepository.findByDocenteIdUsuarioAndPeriodoIdPeriodo(idDocente, idPeriodo)) {
      notasPorFuente.put(calificacion.getFuente(), calificacion.getNotas());
    }
    return new EvaluacionPeriodo(notasPorFuente, buscarPesos(idPeriodo));
  }

  private PesosFuentes buscarPesos(Long idPeriodo) {
    return ponderacionRepository
        .findByPeriodoIdPeriodo(idPeriodo)
        .map(Ponderacion::getPesos)
        .orElseGet(PesosFuentes::predeterminados);
  }
}
