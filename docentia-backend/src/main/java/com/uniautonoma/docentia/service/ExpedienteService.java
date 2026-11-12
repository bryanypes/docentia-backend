package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.CompromisoResponse;
import com.uniautonoma.docentia.dto.ComparativoProgramaResponse;
import com.uniautonoma.docentia.dto.ComparativoProgramaResponse.FuenteComparada;
import com.uniautonoma.docentia.dto.DimensionPorFuenteResponse;
import com.uniautonoma.docentia.dto.DocenteResponse;
import com.uniautonoma.docentia.dto.ExpedienteDocenteResponse;
import com.uniautonoma.docentia.dto.ExpedientePropioResponse;
import com.uniautonoma.docentia.dto.NotaFuenteResponse;
import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.dto.PromedioHistoricoResponse;
import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.dto.ResultadoDimensionResponse;
import com.uniautonoma.docentia.entity.Dimension;
import com.uniautonoma.docentia.entity.EstadoCompromiso;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.CompromisoRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arma el expediente de evaluación de un docente según quién lo consulta. */
@Service
@Transactional(readOnly = true)
public class ExpedienteService {

  private static final int DECIMALES_NOTA_FUENTE = 2;

  private final UsuarioRepository usuarioRepository;
  private final CalificacionRepository calificacionRepository;
  private final CompromisoRepository compromisoRepository;
  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;
  private final PromedioService promedioService;
  private final ProcesoEvaluacionService procesoService;

  public ExpedienteService(
      UsuarioRepository usuarioRepository,
      CalificacionRepository calificacionRepository,
      CompromisoRepository compromisoRepository,
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      PromedioService promedioService,
      ProcesoEvaluacionService procesoService) {
    this.usuarioRepository = usuarioRepository;
    this.calificacionRepository = calificacionRepository;
    this.compromisoRepository = compromisoRepository;
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.promedioService = promedioService;
    this.procesoService = procesoService;
  }

  /**
   * Obtiene el expediente completo de un docente para coordinación o vicerrectoría.
   *
   * @param idDocente identificador del docente
   * @param sesion usuario que consulta; el coordinador solo alcanza docentes de su facultad
   * @return notas por fuente, resultado por dimensión, histórico y compromisos vigentes
   * @throws RecursoNoEncontradoException si el docente no existe o está fuera del alcance
   */
  public ExpedienteDocenteResponse obtenerExpedienteDocente(Long idDocente, UsuarioSesion sesion) {
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    Optional<PeriodoAcademico> periodoActivo = periodoService.buscarPeriodoActivo();
    Optional<EvaluacionPeriodo> evaluacion =
        periodoActivo.map(periodo -> promedioService.cargarEvaluacion(idDocente, periodo));
    return new ExpedienteDocenteResponse(
        DocenteResponse.desde(docente),
        periodoActivo.map(PeriodoResumenResponse::desde).orElse(null),
        periodoActivo
            .map(periodo -> promedioService.obtenerPromedio(idDocente, periodo))
            .orElse(null),
        evaluacion.map(this::listarNotasPorFuente).orElse(List.of()),
        evaluacion.map(this::listarDimensionesPorFuente).orElse(List.of()),
        listarHistorico(idDocente, periodoActivo.map(PeriodoAcademico::getIdPeriodo).orElse(null)),
        listarCompromisosVigentes(idDocente));
  }

  /**
   * Obtiene el expediente del docente autenticado, sin las notas individuales de cada fuente.
   *
   * @param sesion docente que consulta su propia información
   * @return promedio ponderado o aviso de incompleto, resultado por dimensión ya ponderado y
   *     compromisos vigentes
   * @throws RecursoNoEncontradoException si el usuario de la sesión ya no es un docente activo
   */
  public ExpedientePropioResponse obtenerExpedientePropio(UsuarioSesion sesion) {
    Usuario docente =
        usuarioRepository
            .findByIdUsuarioAndRolAndEsActivoTrue(sesion.idUsuario(), Rol.DOCENTE)
            .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado"));
    Optional<PeriodoAcademico> periodoActivo = periodoService.buscarPeriodoActivo();
    Optional<EvaluacionPeriodo> evaluacion =
        periodoActivo.map(
            periodo -> promedioService.cargarEvaluacion(docente.getIdUsuario(), periodo));
    // Hasta la reunión el docente sabe qué falta, pero no su nota (ni por dimensión).
    boolean habilitado =
        periodoActivo
            .map(
                periodo ->
                    procesoService.resultadoHabilitado(
                        docente.getIdUsuario(), periodo.getIdPeriodo()))
            .orElse(true);
    return new ExpedientePropioResponse(
        DocenteResponse.desde(docente),
        periodoActivo.map(PeriodoResumenResponse::desde).orElse(null),
        periodoActivo
            .map(periodo -> promedioService.obtenerPromedio(docente.getIdUsuario(), periodo))
            .map(PromedioPonderadoResponse::sinValorSiEsParcial)
            .map(promedio -> habilitado ? promedio : sinValor(promedio))
            .orElse(null),
        evaluacion
            .map(this::listarResultadosPropios)
            .map(
                dimensiones ->
                    habilitado ? dimensiones : dimensiones.stream().map(this::sinValor).toList())
            .orElse(List.of()),
        listarCompromisosVigentes(docente.getIdUsuario()),
        habilitado);
  }

  /** Comparativo completo dentro del alcance autorizado de coordinación o vicerrectoría. */
  public ComparativoProgramaResponse obtenerComparativoDocente(
      Long idDocente, UsuarioSesion sesion) {
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    PeriodoAcademico periodo =
        periodoService
            .buscarPeriodoActivo()
            .orElseThrow(() -> new ReglaNegocioException("No hay un periodo académico activo"));
    return compararConPrograma(docente, periodo);
  }

  /** El docente obtiene sus notas por fuente solo después de que el resultado esté habilitado. */
  public ComparativoProgramaResponse obtenerComparativoPropio(UsuarioSesion sesion) {
    Usuario docente =
        usuarioRepository
            .findByIdUsuarioAndRolAndEsActivoTrue(sesion.idUsuario(), Rol.DOCENTE)
            .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado"));
    PeriodoAcademico periodo =
        periodoService
            .buscarPeriodoActivo()
            .orElseThrow(() -> new ReglaNegocioException("No hay un periodo académico activo"));
    if (!procesoService.resultadoHabilitado(docente.getIdUsuario(), periodo.getIdPeriodo())) {
      throw new ReglaNegocioException(
          "El comparativo estará disponible cuando la coordinación habilite tu resultado");
    }
    return compararConPrograma(docente, periodo);
  }

  private ComparativoProgramaResponse compararConPrograma(
      Usuario docente, PeriodoAcademico periodo) {
    Map<FuenteEvaluacion, BigDecimal> notasDocente =
        promedioService.cargarEvaluacion(docente.getIdUsuario(), periodo).notasDeFuentes();
    Map<FuenteEvaluacion, List<BigDecimal>> notasPrograma = new EnumMap<>(FuenteEvaluacion.class);
    Arrays.stream(FuenteEvaluacion.values()).forEach(fuente -> notasPrograma.put(fuente, new ArrayList<>()));
    for (Calificacion calificacion :
        calificacionRepository.listarPorProgramaYPeriodo(
            docente.getPrograma().getIdPrograma(), periodo.getIdPeriodo())) {
      BigDecimal nota = calificacion.getNotaFuente();
      if (nota == null && calificacion.getNotas() != null) {
        nota = calificacion.getNotas().promedio();
      }
      if (nota != null) {
        notasPrograma.get(calificacion.getFuente()).add(nota);
      }
    }
    List<FuenteComparada> fuentes =
        Arrays.stream(FuenteEvaluacion.values())
            .map(
                fuente ->
                    new FuenteComparada(
                        fuente,
                        redondear(notasDocente.get(fuente)),
                        promedioRedondeado(notasPrograma.get(fuente))))
            .toList();
    return new ComparativoProgramaResponse(
        PeriodoResumenResponse.desde(periodo), docente.getPrograma().getNombre(), fuentes);
  }

  private BigDecimal promedioRedondeado(List<BigDecimal> notas) {
    if (notas.isEmpty()) return null;
    BigDecimal total = notas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    return total.divide(BigDecimal.valueOf(notas.size()), DECIMALES_NOTA_FUENTE, RoundingMode.HALF_UP);
  }

  private PromedioPonderadoResponse sinValor(PromedioPonderadoResponse promedio) {
    return new PromedioPonderadoResponse(
        promedio.estado(), null, promedio.fuentesPendientes(), null);
  }

  private ResultadoDimensionResponse sinValor(ResultadoDimensionResponse dimension) {
    return new ResultadoDimensionResponse(
        dimension.dimension(), dimension.nombre(), dimension.estado(), null);
  }

  private List<ResultadoDimensionResponse> listarResultadosPropios(EvaluacionPeriodo evaluacion) {
    return Arrays.stream(Dimension.values())
        .map(
            dimension ->
                ResultadoDimensionResponse.desde(
                    dimension,
                    promedioService
                        .calcularResultadoDimension(evaluacion, dimension)
                        .sinValorSiEsParcial()))
        .toList();
  }

  private List<NotaFuenteResponse> listarNotasPorFuente(EvaluacionPeriodo evaluacion) {
    Map<FuenteEvaluacion, BigDecimal> notas = evaluacion.notasDeFuentes();
    return Arrays.stream(FuenteEvaluacion.values())
        .map(
            fuente ->
                new NotaFuenteResponse(
                    fuente, redondear(notas.get(fuente)), evaluacion.pesos().pesoDe(fuente)))
        .toList();
  }

  private List<DimensionPorFuenteResponse> listarDimensionesPorFuente(
      EvaluacionPeriodo evaluacion) {
    return Arrays.stream(Dimension.values())
        .map(
            dimension -> {
              Map<FuenteEvaluacion, BigDecimal> notas = evaluacion.notasDeDimension(dimension);
              PromedioPonderadoResponse resultado =
                  promedioService.calcularResultadoDimension(evaluacion, dimension);
              return new DimensionPorFuenteResponse(
                  dimension,
                  dimension.getNombre(),
                  notas.get(FuenteEvaluacion.ESTUDIANTES),
                  notas.get(FuenteEvaluacion.COORDINACION),
                  notas.get(FuenteEvaluacion.AUTOEVALUACION),
                  resultado.estado(),
                  resultado.valor());
            })
        .toList();
  }

  private List<PromedioHistoricoResponse> listarHistorico(Long idDocente, Long idPeriodoActivo) {
    return calificacionRepository.buscarPeriodosCalificados(idDocente).stream()
        .filter(periodo -> !periodo.getIdPeriodo().equals(idPeriodoActivo))
        .map(
            periodo ->
                new PromedioHistoricoResponse(
                    PeriodoResumenResponse.desde(periodo),
                    promedioService.obtenerPromedio(idDocente, periodo)))
        .toList();
  }

  private List<CompromisoResponse> listarCompromisosVigentes(Long idDocente) {
    return compromisoRepository
        .findByDocenteIdUsuarioAndEstadoOrderByFechaLimiteAsc(idDocente, EstadoCompromiso.VIGENTE)
        .stream()
        .map(CompromisoResponse::desde)
        .toList();
  }

  private BigDecimal redondear(BigDecimal nota) {
    return nota == null ? null : nota.setScale(DECIMALES_NOTA_FUENTE, RoundingMode.HALF_UP);
  }
}
