package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.config.KpiProperties;
import com.uniautonoma.docentia.dto.IndicadorResponse;
import com.uniautonoma.docentia.dto.KpisResponse;
import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.IndicadoresRepository;
import com.uniautonoma.docentia.repository.IndicadoresRepository.ResumenPromedios;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indicadores generales del dashboard: docentes evaluados, promedio general, evaluaciones
 * realizadas y docentes destacados, comparados con el periodo anterior. Se calculan sobre los
 * promedios guardados (D1) con consultas agregadas.
 */
@Service
@Transactional(readOnly = true)
public class KpiService {

  private final IndicadoresRepository indicadoresRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final PeriodoAcademicoService periodoService;
  private final FacultadService facultadService;
  private final KpiProperties propiedades;

  public KpiService(
      IndicadoresRepository indicadoresRepository,
      PeriodoAcademicoRepository periodoRepository,
      PeriodoAcademicoService periodoService,
      FacultadService facultadService,
      KpiProperties propiedades) {
    this.indicadoresRepository = indicadoresRepository;
    this.periodoRepository = periodoRepository;
    this.periodoService = periodoService;
    this.facultadService = facultadService;
    this.propiedades = propiedades;
  }

  /**
   * Calcula los indicadores de un periodo dentro del alcance del usuario.
   *
   * @param idPeriodo periodo consultado, o nulo para el activo
   * @param idFacultad filtro del vicerrector, o nulo para toda la institución; al coordinador
   *     siempre se le aplica su propia facultad
   * @param sesion usuario que consulta
   * @return los cuatro indicadores con su valor anterior y su variación cuando existen
   * @throws RecursoNoEncontradoException si el periodo o la facultad no existen
   * @throws ReglaNegocioException si no se indica periodo y no hay uno activo
   */
  public KpisResponse calcular(Long idPeriodo, Long idFacultad, UsuarioSesion sesion) {
    PeriodoAcademico periodo =
        idPeriodo == null
            ? periodoService.exigirPeriodoActivo()
            : periodoRepository
                .findById(idPeriodo)
                .orElseThrow(
                    () -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
    Long facultadConsulta = sesion.esCoordinador() ? sesion.idFacultad() : idFacultad;
    if (facultadConsulta != null && !sesion.esCoordinador()) {
      facultadService.buscarFacultadActiva(facultadConsulta);
    }

    Datos actual = leer(periodo.getIdPeriodo(), facultadConsulta);
    Optional<PeriodoAcademico> anterior =
        periodoRepository.findFirstByFechaInicioLessThanOrderByFechaInicioDesc(
            periodo.getFechaInicio());
    // Un periodo anterior sin ninguna calificación en el alcance no tiene datos que comparar: sus
    // valores van nulos, no en cero.
    Datos previo =
        anterior
            .map(periodoAnterior -> leer(periodoAnterior.getIdPeriodo(), facultadConsulta))
            .filter(datos -> datos.evaluaciones() > 0)
            .orElse(Datos.SIN_DATOS);

    return new KpisResponse(
        PeriodoResumenResponse.desde(periodo),
        anterior.map(PeriodoResumenResponse::desde).orElse(null),
        facultadConsulta,
        propiedades.destacadoUmbral(),
        IndicadorResponse.comparar(numero(actual.evaluados()), numero(previo.evaluados())),
        IndicadorResponse.comparar(actual.promedio(), previo.promedio()),
        IndicadorResponse.comparar(numero(actual.evaluaciones()), numero(previo.evaluaciones())),
        IndicadorResponse.comparar(numero(actual.destacados()), numero(previo.destacados())));
  }

  private Datos leer(Long idPeriodo, Long idFacultad) {
    ResumenPromedios resumen =
        indicadoresRepository.resumirPromedios(
            idPeriodo, idFacultad, propiedades.destacadoUmbral());
    return new Datos(
        resumen.getDocentesEvaluados(),
        resumen.getPromedioGeneral(),
        indicadoresRepository.contarCalificaciones(idPeriodo, idFacultad),
        resumen.getDocentesDestacados());
  }

  private static BigDecimal numero(Long valor) {
    return valor == null ? null : BigDecimal.valueOf(valor);
  }

  private record Datos(Long evaluados, BigDecimal promedio, Long evaluaciones, Long destacados) {
    static final Datos SIN_DATOS = new Datos(null, null, null, null);
  }
}
