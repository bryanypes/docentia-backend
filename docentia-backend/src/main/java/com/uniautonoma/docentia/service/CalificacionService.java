package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.CalificacionRequest;
import com.uniautonoma.docentia.dto.CalificacionResponse;
import com.uniautonoma.docentia.dto.CorreccionCalificacionResponse;
import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.CorreccionCalificacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.Importacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.EtapaCerradaException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.CorreccionCalificacionRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Registro y consulta de las calificaciones de un docente. Cada registro recalcula y guarda su
 * promedio del periodo, y si cambia las notas de una calificación existente deja la corrección en
 * el historial.
 */
@Service
@Transactional
public class CalificacionService {

  private final CalificacionRepository calificacionRepository;
  private final CorreccionCalificacionRepository correccionRepository;
  private final UsuarioRepository usuarioRepository;
  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;
  private final PromedioService promedioService;
  private final VentanaEtapaService ventanaEtapaService;

  public CalificacionService(
      CalificacionRepository calificacionRepository,
      CorreccionCalificacionRepository correccionRepository,
      UsuarioRepository usuarioRepository,
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      PromedioService promedioService,
      VentanaEtapaService ventanaEtapaService) {
    this.calificacionRepository = calificacionRepository;
    this.correccionRepository = correccionRepository;
    this.usuarioRepository = usuarioRepository;
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.promedioService = promedioService;
    this.ventanaEtapaService = ventanaEtapaService;
  }

  /**
   * Guarda la calificación de una fuente para un docente en el periodo activo. Si esa fuente ya
   * había calificado, la reemplaza y, si las notas cambiaron, registra la corrección.
   *
   * @param idDocente identificador del docente calificado
   * @param fuente fuente a nombre de la cual se califica
   * @param solicitud notas de los cinco aspectos, observación y motivo opcionales
   * @param sesion usuario que califica; su alcance decide qué docentes puede calificar
   * @return la calificación guardada
   * @throws RecursoNoEncontradoException si el docente no existe o está fuera del alcance
   * @throws ReglaNegocioException si no hay un periodo académico activo
   * @throws EtapaCerradaException si hoy no está abierta la recolección de calificaciones
   */
  public CalificacionResponse calificarDocente(
      Long idDocente,
      FuenteEvaluacion fuente,
      CalificacionRequest solicitud,
      UsuarioSesion sesion) {
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    ventanaEtapaService.exigirEtapaAbierta(periodo, TipoEtapa.RECOLECCION_CALIFICACIONES);
    Usuario evaluador = usuarioRepository.getReferenceById(sesion.idUsuario());
    Calificacion calificacion =
        guardar(
            docente,
            periodo,
            fuente,
            solicitud.aNotasAspectos(),
            textoOpcional(solicitud.observacion()),
            evaluador,
            evaluador,
            textoOpcional(solicitud.motivoCorreccion()),
            null);
    return CalificacionResponse.desde(calificacion);
  }

  /**
   * Guarda la nota consolidada de los estudiantes, como lo hace un proceso del sistema (seed).
   *
   * @see #registrarCalificacionEstudiantes(Usuario, PeriodoAcademico, NotasAspectos, Long, String,
   *     Importacion)
   */
  public ResultadoRegistro registrarCalificacionEstudiantes(
      Usuario docente, PeriodoAcademico periodo, NotasAspectos notas) {
    return registrarCalificacionEstudiantes(docente, periodo, notas, null, null, null);
  }

  /**
   * Guarda la nota consolidada de los estudiantes para un docente en un periodo. La usan la carga
   * de CSV y el seed; no revisa la ventana de la etapa, eso lo hace quien la llama.
   *
   * @param docente docente evaluado
   * @param periodo periodo al que corresponde la evaluación
   * @param notas promedio de los cinco aspectos que dieron los estudiantes
   * @param idResponsable usuario que hizo la carga, o nulo si es un proceso del sistema
   * @param motivo texto que queda en el historial si la nota cambia
   * @param importacion carga de CSV de la que sale la nota, o nula
   * @return si la nota era nueva, cambió o quedó igual; si quedó igual no se toca nada y la nota
   *     sigue apuntando a la carga que fijó su valor
   */
  public ResultadoRegistro registrarCalificacionEstudiantes(
      Usuario docente,
      PeriodoAcademico periodo,
      NotasAspectos notas,
      Long idResponsable,
      String motivo,
      Importacion importacion) {
    Usuario responsable =
        idResponsable == null ? null : usuarioRepository.getReferenceById(idResponsable);
    Optional<Calificacion> anterior =
        calificacionRepository.findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
            docente.getIdUsuario(), periodo.getIdPeriodo(), FuenteEvaluacion.ESTUDIANTES);
    if (anterior.isPresent() && anterior.get().getNotas().mismasNotas(notas)) {
      return ResultadoRegistro.SIN_CAMBIOS;
    }
    guardar(
        docente,
        periodo,
        FuenteEvaluacion.ESTUDIANTES,
        notas,
        null,
        null,
        responsable,
        motivo,
        importacion);
    return anterior.isPresent() ? ResultadoRegistro.CORREGIDA : ResultadoRegistro.NUEVA;
  }

  /**
   * Consulta la calificación que una fuente le dio a un docente en el periodo activo.
   *
   * @param idDocente identificador del docente
   * @param fuente fuente consultada
   * @param sesion usuario que consulta
   * @return la calificación, o vacío si aún no se ha registrado o no hay periodo activo
   * @throws RecursoNoEncontradoException si el docente no existe o está fuera del alcance
   */
  @Transactional(readOnly = true)
  public Optional<CalificacionResponse> obtenerCalificacion(
      Long idDocente, FuenteEvaluacion fuente, UsuarioSesion sesion) {
    docenteService.buscarDocenteVisible(idDocente, sesion);
    return periodoService
        .buscarPeriodoActivo()
        .flatMap(
            periodo ->
                calificacionRepository.findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                    idDocente, periodo.getIdPeriodo(), fuente))
        .map(CalificacionResponse::desde);
  }

  /**
   * Lista el historial de correcciones de un docente.
   *
   * @param idDocente identificador del docente
   * @param idPeriodo periodo por el que se filtra, o nulo para todos
   * @param sesion usuario que consulta; el coordinador solo alcanza docentes de su facultad
   * @return correcciones de la más reciente a la más antigua
   * @throws RecursoNoEncontradoException si el docente no existe o está fuera del alcance
   */
  @Transactional(readOnly = true)
  public List<CorreccionCalificacionResponse> listarCorrecciones(
      Long idDocente, Long idPeriodo, UsuarioSesion sesion) {
    docenteService.buscarDocenteVisible(idDocente, sesion);
    return correccionRepository.buscarPorDocente(idDocente, idPeriodo).stream()
        .map(CorreccionCalificacionResponse::desde)
        .toList();
  }

  private Calificacion guardar(
      Usuario docente,
      PeriodoAcademico periodo,
      FuenteEvaluacion fuente,
      NotasAspectos notas,
      String observacion,
      Usuario evaluador,
      Usuario responsable,
      String motivo,
      Importacion importacion) {
    Calificacion calificacion =
        calificacionRepository
            .findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                docente.getIdUsuario(), periodo.getIdPeriodo(), fuente)
            .orElseGet(() -> new Calificacion(docente, periodo, fuente));
    boolean esCorreccion =
        calificacion.getIdCalificacion() != null && !calificacion.getNotas().mismasNotas(notas);
    BigDecimal notaAnterior = esCorreccion ? calificacion.getNotas().promedio() : null;
    PromedioPonderadoResponse promedioAnterior =
        esCorreccion
            ? promedioService.obtenerPromedio(docente.getIdUsuario(), periodo.getIdPeriodo())
            : null;

    calificacion.registrar(notas, observacion, evaluador, importacion);
    Calificacion guardada = calificacionRepository.saveAndFlush(calificacion);
    PromedioPonderadoResponse promedioNuevo =
        promedioService.recalcular(docente.getIdUsuario(), periodo.getIdPeriodo());

    if (esCorreccion) {
      correccionRepository.save(
          new CorreccionCalificacion(
              guardada, notaAnterior, promedioAnterior, promedioNuevo, responsable, motivo));
    }
    return guardada;
  }

  private String textoOpcional(String texto) {
    return StringUtils.hasText(texto) ? texto.trim() : null;
  }

  /** Lo que pasó con una calificación al registrarla. */
  public enum ResultadoRegistro {
    NUEVA,
    CORREGIDA,
    SIN_CAMBIOS
  }
}
