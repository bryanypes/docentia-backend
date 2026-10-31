package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.PonderacionRequest;
import com.uniautonoma.docentia.dto.PonderacionResponse;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.PesosFuentes;
import com.uniautonoma.docentia.entity.Ponderacion;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.PonderacionRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Configuración del peso de cada fuente de evaluación en un periodo. */
@Service
@Transactional
public class PonderacionService {

  private final PonderacionRepository ponderacionRepository;
  private final PeriodoAcademicoService periodoService;
  private final PromedioService promedioService;

  public PonderacionService(
      PonderacionRepository ponderacionRepository,
      PeriodoAcademicoService periodoService,
      PromedioService promedioService) {
    this.ponderacionRepository = ponderacionRepository;
    this.periodoService = periodoService;
    this.promedioService = promedioService;
  }

  /**
   * Consulta los pesos vigentes del periodo activo.
   *
   * @return pesos configurados, o los predeterminados si nadie los ha cambiado
   * @throws ReglaNegocioException si no hay un periodo académico activo
   */
  @Transactional(readOnly = true)
  public PonderacionResponse obtenerPonderacionActiva() {
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    Optional<Ponderacion> configurada =
        ponderacionRepository.findByPeriodoIdPeriodo(periodo.getIdPeriodo());
    return PonderacionResponse.desde(
        periodo,
        configurada.map(Ponderacion::getPesos).orElseGet(PesosFuentes::predeterminados),
        configurada.isEmpty());
  }

  /**
   * Fija los pesos de las tres fuentes para el periodo activo y recalcula los promedios guardados
   * de ese periodo.
   *
   * @param solicitud pesos de estudiantes, coordinación y vicerrectoría
   * @return la ponderación guardada
   * @throws DatosInvalidosException si los tres pesos no suman 100
   * @throws ReglaNegocioException si no hay un periodo académico activo
   */
  public PonderacionResponse configurarPonderacionActiva(PonderacionRequest solicitud) {
    PesosFuentes pesos = solicitud.aPesosFuentes();
    if (!pesos.sumanCien()) {
      throw new DatosInvalidosException("Las ponderaciones deben sumar 100%");
    }
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    Ponderacion ponderacion =
        ponderacionRepository
            .findByPeriodoIdPeriodo(periodo.getIdPeriodo())
            .orElseGet(() -> new Ponderacion(periodo, pesos));
    ponderacion.setPesos(pesos);
    ponderacionRepository.saveAndFlush(ponderacion);
    promedioService.recalcularPeriodo(periodo.getIdPeriodo());
    return PonderacionResponse.desde(periodo, pesos, false);
  }
}
