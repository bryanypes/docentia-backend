package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.config.KpiProperties;
import com.uniautonoma.docentia.dto.IndicadorResponse;
import com.uniautonoma.docentia.dto.KpisResponse;
import com.uniautonoma.docentia.dto.PanelIndicadoresResponse;
import com.uniautonoma.docentia.dto.PanelIndicadoresResponse.ConteoCategoria;
import com.uniautonoma.docentia.dto.PanelIndicadoresResponse.FilaPanel;
import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.entity.CategoriaDesempeno;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.repository.IndicadoresRepository;
import com.uniautonoma.docentia.repository.IndicadoresRepository.ResumenPrograma;
import com.uniautonoma.docentia.repository.IndicadoresRepository.ResumenPromedios;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
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
  private final ProgramaRepository programaRepository;
  private final KpiProperties propiedades;

  public KpiService(
      IndicadoresRepository indicadoresRepository,
      PeriodoAcademicoRepository periodoRepository,
      PeriodoAcademicoService periodoService,
      FacultadService facultadService,
      ProgramaRepository programaRepository,
      KpiProperties propiedades) {
    this.indicadoresRepository = indicadoresRepository;
    this.periodoRepository = periodoRepository;
    this.periodoService = periodoService;
    this.facultadService = facultadService;
    this.programaRepository = programaRepository;
    this.propiedades = propiedades;
  }

  /**
   * Arma el panel agregado por programa (I11). Promedio, cobertura y distribución cuentan solo los
   * promedios completos, igual que los indicadores generales.
   *
   * @param idPeriodo periodo consultado, o nulo para el activo
   * @param idFacultad filtro del vicerrector; al coordinador siempre se le aplica su facultad
   * @param sesion usuario que consulta
   * @return una fila por programa con docentes activos y el total del alcance
   */
  public PanelIndicadoresResponse armarPanel(
      Long idPeriodo, Long idFacultad, UsuarioSesion sesion) {
    PeriodoAcademico periodo = resolverPeriodo(idPeriodo);
    List<ResumenPrograma> filas =
        indicadoresRepository.resumirPorPrograma(
            periodo.getIdPeriodo(), resolverFacultad(idFacultad, sesion));
    long[] conteoTotal = new long[CategoriaDesempeno.values().length];
    filas.forEach(
        fila -> {
          long[] conteo = conteos(fila);
          for (int i = 0; i < conteo.length; i++) {
            conteoTotal[i] += conteo[i];
          }
        });
    return new PanelIndicadoresResponse(
        PeriodoResumenResponse.desde(periodo),
        filas.stream()
            .map(
                fila ->
                    fila(
                        fila.getIdPrograma(),
                        fila.getPrograma(),
                        fila.getFacultad(),
                        fila.getDocentes(),
                        fila.getEvaluados(),
                        fila.getSumaNotas(),
                        conteos(fila)))
            .toList(),
        fila(
            null,
            "Total",
            null,
            filas.stream().mapToLong(ResumenPrograma::getDocentes).sum(),
            filas.stream().mapToLong(ResumenPrograma::getEvaluados).sum(),
            filas.stream()
                .map(ResumenPrograma::getSumaNotas)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add),
            conteoTotal));
  }

  /** Conteos en el orden de {@link CategoriaDesempeno}. */
  private static long[] conteos(ResumenPrograma fila) {
    return Arrays.stream(CategoriaDesempeno.values())
        .mapToLong(
            categoria ->
                switch (categoria) {
                  case MUY_ALTO -> fila.getMuyAlto();
                  case ALTO -> fila.getAlto();
                  case ACEPTABLE -> fila.getAceptable();
                  case BAJO -> fila.getBajo();
                  case MUY_BAJO -> fila.getMuyBajo();
                })
        .toArray();
  }

  private static FilaPanel fila(
      Long idPrograma,
      String programa,
      String facultad,
      long docentes,
      long evaluados,
      BigDecimal sumaNotas,
      long[] conteos) {
    CategoriaDesempeno[] categorias = CategoriaDesempeno.values();
    return new FilaPanel(
        idPrograma,
        programa,
        facultad,
        docentes,
        evaluados,
        porcentaje(evaluados, docentes),
        evaluados == 0
            ? null
            : sumaNotas.divide(BigDecimal.valueOf(evaluados), 2, RoundingMode.HALF_UP),
        IntStream.range(0, categorias.length)
            .mapToObj(
                i ->
                    new ConteoCategoria(
                        categorias[i].name(),
                        categorias[i].getEtiqueta(),
                        conteos[i],
                        porcentaje(conteos[i], evaluados)))
            .toList());
  }

  private static BigDecimal porcentaje(long parte, long total) {
    return total == 0
        ? BigDecimal.ZERO.setScale(1)
        : BigDecimal.valueOf(parte * 100)
            .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
  }

  private PeriodoAcademico resolverPeriodo(Long idPeriodo) {
    return idPeriodo == null
        ? periodoService.exigirPeriodoActivo()
        : periodoRepository
            .findById(idPeriodo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
  }

  private Long resolverFacultad(Long idFacultad, UsuarioSesion sesion) {
    if (sesion.esCoordinador()) {
      return sesion.idFacultad();
    }
    if (idFacultad != null) {
      facultadService.buscarFacultadActiva(idFacultad);
    }
    return idFacultad;
  }

  /**
   * Calcula los indicadores de un periodo dentro del alcance del usuario.
   *
   * @param idPeriodo periodo consultado, o nulo para el activo
   * @param idFacultad filtro del vicerrector, o nulo para toda la institución; al coordinador
   *     siempre se le aplica su propia facultad
   * @param idPrograma filtro opcional por programa (carrera) dentro de ese alcance
   * @param sesion usuario que consulta
   * @return los cuatro indicadores con su valor anterior y su variación cuando existen
   * @throws RecursoNoEncontradoException si el periodo, la facultad o el programa no existen, o el
   *     programa queda fuera del alcance
   * @throws ReglaNegocioException si no se indica periodo y no hay uno activo
   */
  public KpisResponse calcular(
      Long idPeriodo, Long idFacultad, Long idPrograma, UsuarioSesion sesion) {
    PeriodoAcademico periodo = resolverPeriodo(idPeriodo);
    Long facultadConsulta = resolverFacultad(idFacultad, sesion);
    Long programaConsulta = resolverPrograma(idPrograma, facultadConsulta);

    Datos actual = leer(periodo.getIdPeriodo(), facultadConsulta, programaConsulta);
    Optional<PeriodoAcademico> anterior =
        periodoRepository.findFirstByFechaInicioLessThanOrderByFechaInicioDesc(
            periodo.getFechaInicio());
    // Un periodo anterior sin ninguna calificación en el alcance no tiene datos que comparar: sus
    // valores van nulos, no en cero.
    Datos previo =
        anterior
            .map(
                periodoAnterior ->
                    leer(periodoAnterior.getIdPeriodo(), facultadConsulta, programaConsulta))
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

  /**
   * Un programa solo vale si está activo y, cuando hay facultad en la consulta, pertenece a ella.
   *
   * @return el programa a filtrar, o nulo para todos los del alcance
   */
  private Long resolverPrograma(Long idPrograma, Long idFacultad) {
    if (idPrograma == null) {
      return null;
    }
    Programa programa =
        programaRepository
            .findByIdProgramaAndEsActivoTrue(idPrograma)
            .filter(
                p -> idFacultad == null || p.getFacultad().getIdFacultad().equals(idFacultad))
            .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado"));
    return programa.getIdPrograma();
  }

  private Datos leer(Long idPeriodo, Long idFacultad, Long idPrograma) {
    ResumenPromedios resumen =
        indicadoresRepository.resumirPromedios(
            idPeriodo, idFacultad, idPrograma, propiedades.destacadoUmbral());
    return new Datos(
        resumen.getDocentesEvaluados(),
        resumen.getPromedioGeneral(),
        indicadoresRepository.contarCalificaciones(idPeriodo, idFacultad, idPrograma),
        resumen.getDocentesDestacados());
  }

  private static BigDecimal numero(Long valor) {
    return valor == null ? null : BigDecimal.valueOf(valor);
  }

  private record Datos(Long evaluados, BigDecimal promedio, Long evaluaciones, Long destacados) {
    static final Datos SIN_DATOS = new Datos(null, null, null, null);
  }
}
