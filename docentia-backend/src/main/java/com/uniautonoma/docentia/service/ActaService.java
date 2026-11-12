package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.config.ActasProperties;
import com.uniautonoma.docentia.dto.ActaResponse;
import com.uniautonoma.docentia.dto.CompromisoActaRequest;
import com.uniautonoma.docentia.dto.EstadoPromedio;
import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.dto.VerificacionActaResponse;
import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.CorreccionCalificacion;
import com.uniautonoma.docentia.entity.EstadoActa;
import com.uniautonoma.docentia.entity.FirmaActa;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoActa;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.exception.NoProcesableException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.ActaRepository;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.CorreccionCalificacionRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo del acta: la coordinación la abre y registra compromisos, docente y coordinación la firman
 * y con la segunda firma se genera el PDF, una sola vez. Firmada es inmutable.
 */
@Service
@Transactional
public class ActaService {

  private final ApplicationEventPublisher eventos;

  private static final SecureRandom ALEATORIO = new SecureRandom();
  private static final String ENLACE_ACTA = "/mi-acta";
  private static final String ENTIDAD_ACTA = "ACTA";

  private final ActaRepository actaRepository;
  private final CitaRepository citaRepository;
  private final CorreccionCalificacionRepository correccionRepository;
  private final UsuarioRepository usuarioRepository;
  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;
  private final PromedioService promedioService;
  private final VentanaEtapaService ventanaEtapaService;
  private final GeneradorPdfActa generadorPdf;
  private final ActasProperties propiedades;
  private final ProcesoEvaluacionService procesoService;
  private final Clock reloj;

  public ActaService(
      ActaRepository actaRepository,
      CitaRepository citaRepository,
      CorreccionCalificacionRepository correccionRepository,
      UsuarioRepository usuarioRepository,
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      PromedioService promedioService,
      VentanaEtapaService ventanaEtapaService,
      GeneradorPdfActa generadorPdf,
      ActasProperties propiedades,
      ProcesoEvaluacionService procesoService,
      Clock reloj,
      ApplicationEventPublisher eventos) {
    this.eventos = eventos;
    this.actaRepository = actaRepository;
    this.citaRepository = citaRepository;
    this.correccionRepository = correccionRepository;
    this.usuarioRepository = usuarioRepository;
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.promedioService = promedioService;
    this.ventanaEtapaService = ventanaEtapaService;
    this.generadorPdf = generadorPdf;
    this.propiedades = propiedades;
    this.procesoService = procesoService;
    this.reloj = reloj;
  }

  /**
   * Abre el acta del docente en el periodo activo.
   *
   * @param idDocente docente del acta
   * @param sesion coordinador con alcance sobre el docente
   * @return el acta en borrador
   * @throws ReglaNegocioException si el docente ya tiene un acta vigente en el periodo
   */
  public ActaResponse crear(Long idDocente, UsuarioSesion sesion) {
    return crear(idDocente, null, sesion);
  }

  /**
   * Abre el acta del docente en el periodo activo con el tipo que elige la coordinación.
   *
   * @param tipo de compromiso o de retroalimentación; nulo deja el que corresponde a la nota
   * @throws ReglaNegocioException si ya tiene un acta vigente o pide retroalimentación con una nota
   *     bajo el umbral
   */
  public ActaResponse crear(Long idDocente, TipoActa tipo, UsuarioSesion sesion) {
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    if (actaRepository.existsByDocenteIdUsuarioAndPeriodoIdPeriodoAndEstadoNot(
        idDocente, periodo.getIdPeriodo(), EstadoActa.ANULADA)) {
      throw new ReglaNegocioException("El docente ya tiene un acta en este periodo");
    }
    Acta acta = new Acta(docente, periodo, propiedades.compromisos().umbral());
    congelar(acta);
    if (tipo != null) {
      acta.cambiarTipo(tipo);
    }
    Acta guardada = actaRepository.saveAndFlush(acta);
    avisarSiEstaLista(guardada);
    return ActaResponse.desde(guardada);
  }

  /**
   * Lista las actas con todas sus versiones.
   *
   * @param idDocente docente consultado; el rol docente solo consulta las propias
   * @param sesion usuario que consulta
   * @return versiones de la más reciente a la más antigua
   */
  @Transactional(readOnly = true)
  public List<ActaResponse> listar(Long idDocente, UsuarioSesion sesion) {
    Long id = sesion.esDocente() ? sesion.idUsuario() : idDocente;
    if (id == null) {
      throw new DatosInvalidosException("Indica el docente");
    }
    if (!sesion.esDocente()) {
      docenteService.buscarDocenteVisible(id, sesion);
    }
    return actaRepository
        .findByDocenteIdUsuarioOrderByPeriodoFechaInicioDescVersionDesc(id)
        .stream()
        .filter(acta -> !sesion.esDocente() || visibleParaDocente(acta))
        .map(acta -> sesion.esDocente() ? ActaResponse.paraDocente(acta) : ActaResponse.desde(acta))
        .toList();
  }

  @Transactional(readOnly = true)
  public ActaResponse obtener(Long idActa, UsuarioSesion sesion) {
    Acta acta = buscarVisible(idActa, sesion);
    return sesion.esDocente() ? ActaResponse.paraDocente(acta) : ActaResponse.desde(acta);
  }

  /**
   * Cambia el tipo del acta mientras nadie la haya firmado.
   *
   * @throws ReglaNegocioException si ya tiene firmas o la nota exige el acta de compromiso
   */
  public ActaResponse cambiarTipo(Long idActa, TipoActa tipo, UsuarioSesion sesion) {
    Acta acta = buscarVisible(idActa, sesion);
    acta.cambiarTipo(tipo);
    Acta guardada = actaRepository.saveAndFlush(acta);
    avisarSiEstaLista(guardada);
    return ActaResponse.desde(guardada);
  }

  /**
   * Agrega un compromiso al acta en borrador.
   *
   * @throws ReglaNegocioException si el acta ya está firmada
   * @throws DatosInvalidosException si el plazo ya pasó
   */
  public ActaResponse agregarCompromiso(
      Long idActa, CompromisoActaRequest solicitud, UsuarioSesion sesion) {
    Acta acta = buscarVisible(idActa, sesion);
    acta.exigirBorrador();
    exigirSinFirmas(acta);
    if (solicitud.fechaLimite().isBefore(LocalDateTime.now(reloj).toLocalDate())) {
      throw new DatosInvalidosException("El plazo no puede estar en el pasado", "fechaLimite");
    }
    acta.agregarCompromiso(solicitud.descripcion().trim(), solicitud.fechaLimite());
    Acta guardada = actaRepository.saveAndFlush(acta);
    avisarSiEstaLista(guardada);
    return ActaResponse.desde(guardada);
  }

  /** Quita un compromiso del acta en borrador. */
  public ActaResponse quitarCompromiso(Long idActa, Long idCompromiso, UsuarioSesion sesion) {
    Acta acta = buscarVisible(idActa, sesion);
    acta.exigirBorrador();
    exigirSinFirmas(acta);
    Compromiso compromiso =
        acta.getCompromisos().stream()
            .filter(actual -> actual.getIdCompromiso().equals(idCompromiso))
            .findFirst()
            .orElseThrow(() -> new RecursoNoEncontradoException("Compromiso no encontrado"));
    acta.quitarCompromiso(compromiso);
    return ActaResponse.desde(actaRepository.saveAndFlush(acta));
  }

  /**
   * Firma el acta. La firma es irreversible y exige confirmación explícita.
   *
   * @param idActa acta a firmar
   * @param confirmacion debe ser verdadero
   * @param ip dirección desde la que se firma
   * @param sesion docente dueño del acta o coordinador con alcance
   * @return el acta; con la segunda firma queda FIRMADA y con su PDF
   * @throws NoProcesableException si falta la confirmación o los compromisos que exige el umbral
   * @throws ReglaNegocioException si no hay cita agendada, el promedio no está completo o ya firmó
   */
  public ActaResponse firmar(Long idActa, boolean confirmacion, String ip, UsuarioSesion sesion) {
    return firmar(idActa, confirmacion, null, ip, sesion);
  }

  /**
   * Firma el acta con el trazo dibujado en la página, que se imprime en el PDF.
   *
   * @param trazo PNG como data URL; obligatorio si docentia.actas.exigir-firma-dibujada
   * @throws NoProcesableException si falta la confirmación, el trazo o los compromisos exigidos
   */
  public ActaResponse firmar(
      Long idActa, boolean confirmacion, String trazo, String ip, UsuarioSesion sesion) {
    Acta acta = buscarVisible(idActa, sesion);
    acta.exigirBorrador();
    if (!confirmacion) {
      throw new NoProcesableException("Confirma que leíste el acta; la firma es irreversible");
    }
    ventanaEtapaService.exigirEtapaAbierta(acta.getPeriodo(), TipoEtapa.FIRMA_ACTAS);
    Long idDocente = acta.getDocente().getIdUsuario();
    Long idPeriodo = acta.getPeriodo().getIdPeriodo();
    if (citaRepository.buscarActiva(idDocente, idPeriodo).isEmpty()) {
      throw new ReglaNegocioException(
          "El acta se firma después de agendar la cita de retroalimentación");
    }
    if (!procesoService.resultadoHabilitado(idDocente, idPeriodo)) {
      throw new ReglaNegocioException(
          "El acta se firma en la reunión, después de que la coordinación registre la asistencia");
    }
    if (acta.firmadaPor(sesion.rol())) {
      throw new ReglaNegocioException("Ya firmaste esta acta");
    }
    congelar(acta);
    if (acta.getEstadoPromedio() != EstadoPromedio.COMPLETO) {
      throw new ReglaNegocioException(
          "El acta se firma con el promedio completo de los tres evaluadores");
    }
    if (acta.requiereCompromisos() && acta.getCompromisos().isEmpty()) {
      throw new NoProcesableException(
          "Con una nota menor a "
              + acta.getUmbralCompromisos()
              + " el acta exige al menos un compromiso con plazo");
    }
    byte[] png =
        trazo != null || propiedades.actas().exigirFirmaDibujada() ? TrazoFirma.leer(trazo) : null;
    LocalDateTime ahora = LocalDateTime.now(reloj);
    FirmaActa firma =
        new FirmaActa(
            acta,
            sesion.idUsuario(),
            sesion.rol(),
            sesion.nombreCompleto(),
            sesion.correo(),
            ip,
            ahora);
    acta.registrarFirma(png == null ? firma : firma.conTrazo(png, generadorPdf.sha256(png)));
    if (acta.firmadaPor(Rol.DOCENTE) && acta.firmadaPor(Rol.COORDINADOR)) {
      cerrar(acta, ahora);
    }
    Acta guardada = actaRepository.saveAndFlush(acta);
    avisarFirma(guardada, sesion);
    return ActaResponse.desde(guardada);
  }

  /**
   * Corrige un acta firmada: la anula y crea la versión siguiente en borrador, que se firma otra
   * vez. El PDF anterior se conserva.
   *
   * @param idActa acta firmada
   * @param motivo motivo obligatorio
   * @param sesion usuario con el rol configurado en docentia.actas.rol-corrector
   * @return la versión nueva
   */
  public ActaResponse corregir(Long idActa, String motivo, UsuarioSesion sesion) {
    if (sesion.rol() != propiedades.actas().rolCorrector()) {
      throw new AccessDeniedException("Este rol no corrige actas");
    }
    Acta anterior = buscarVisible(idActa, sesion);
    if (anterior.getEstado() != EstadoActa.FIRMADA) {
      throw new ReglaNegocioException("Solo se corrige un acta firmada");
    }
    anterior.anular();
    actaRepository.saveAndFlush(anterior);
    Acta nueva = Acta.nuevaVersion(anterior, motivo.trim(), sesion.idUsuario());
    congelar(nueva);
    Acta guardada = actaRepository.saveAndFlush(nueva);
    eventos.publishEvent(
        EventoNotificacion.aUsuario(
            TipoNotificacion.ACTA_CORREGIDA,
            guardada.getDocente().getIdUsuario(),
            ENLACE_ACTA,
            ENTIDAD_ACTA,
            guardada.getIdActa(),
            "acta:" + guardada.getIdActa()));
    return ActaResponse.desde(guardada);
  }

  /** PDF guardado del acta; nunca se regenera. */
  @Transactional(readOnly = true)
  public PdfActa descargarPdf(Long idActa, UsuarioSesion sesion) {
    Acta acta = buscarVisible(idActa, sesion);
    if (acta.getPdf() == null) {
      throw new ReglaNegocioException("El acta todavía no está firmada por las dos partes");
    }
    return new PdfActa(
        acta.getPdf(), "acta-" + acta.getPeriodo().getNombre() + "-v" + acta.getVersion() + ".pdf");
  }

  /** Verificación pública por código: sin notas, solo si el documento es auténtico y vigente. */
  @Transactional(readOnly = true)
  public VerificacionActaResponse verificar(String codigo) {
    return actaRepository
        .findByCodigoVerificacion(codigo.trim().toUpperCase(Locale.ROOT))
        .map(VerificacionActaResponse::desde)
        .orElseThrow(() -> new RecursoNoEncontradoException("Acta no encontrada"));
  }

  public record PdfActa(byte[] contenido, String nombre) {}

  private void cerrar(Acta acta, LocalDateTime ahora) {
    String codigo = generarCodigo();
    String hashContenido = generadorPdf.hashContenido(acta, codigo);
    byte[] pdf = generadorPdf.generar(acta, codigo, hashContenido);
    acta.cerrar(codigo, hashContenido, generadorPdf.sha256(pdf), pdf, ahora);
  }

  private String generarCodigo() {
    byte[] bytes = new byte[6];
    ALEATORIO.nextBytes(bytes);
    return "DOC-" + HexFormat.of().formatHex(bytes).toUpperCase(Locale.ROOT);
  }

  private void congelar(Acta acta) {
    Long idDocente = acta.getDocente().getIdUsuario();
    PeriodoAcademico periodo = acta.getPeriodo();
    PromedioPonderadoResponse promedio = promedioService.obtenerPromedio(idDocente, periodo);
    Map<FuenteEvaluacion, BigDecimal> notas =
        promedioService.cargarEvaluacion(idDocente, periodo).notasDeFuentes();
    Long idCorreccion =
        correccionRepository
            .findFirstByDocenteIdUsuarioAndPeriodoIdPeriodoOrderByIdCorreccionDesc(
                idDocente, periodo.getIdPeriodo())
            .map(CorreccionCalificacion::getIdCorreccion)
            .orElse(null);
    acta.congelarResultado(
        promedio.valor(),
        promedio.estado(),
        promedio.categoria() == null ? null : promedio.categoria().codigo(),
        tresDecimales(notas.get(FuenteEvaluacion.ESTUDIANTES)),
        tresDecimales(notas.get(FuenteEvaluacion.COORDINACION)),
        tresDecimales(notas.get(FuenteEvaluacion.AUTOEVALUACION)),
        idCorreccion);
  }

  private BigDecimal tresDecimales(BigDecimal nota) {
    return nota == null ? null : nota.setScale(3, RoundingMode.HALF_UP);
  }

  /** El borrador del periodo se le muestra al docente solo con el resultado habilitado. */
  private boolean visibleParaDocente(Acta acta) {
    return acta.getEstado() != EstadoActa.BORRADOR
        || procesoService.resultadoHabilitado(
            acta.getDocente().getIdUsuario(), acta.getPeriodo().getIdPeriodo());
  }

  private void exigirSinFirmas(Acta acta) {
    if (!acta.getFirmas().isEmpty()) {
      throw new ReglaNegocioException("El acta ya tiene una firma: los compromisos no cambian");
    }
  }

  private Acta buscarVisible(Long idActa, UsuarioSesion sesion) {
    Acta acta =
        actaRepository
            .findById(idActa)
            .orElseThrow(() -> new RecursoNoEncontradoException("Acta no encontrada"));
    Long idDocente = acta.getDocente().getIdUsuario();
    if (sesion.esDocente()) {
      if (!idDocente.equals(sesion.idUsuario()) || !visibleParaDocente(acta)) {
        throw new RecursoNoEncontradoException("Acta no encontrada");
      }
      return acta;
    }
    try {
      docenteService.buscarDocenteVisible(idDocente, sesion);
    } catch (RecursoNoEncontradoException fueraDeAlcance) {
      throw new RecursoNoEncontradoException("Acta no encontrada");
    }
    return acta;
  }

  /**
   * El docente se entera cuando el acta ya se puede firmar: con el compromiso exigido, si aplica.
   */
  private void avisarSiEstaLista(Acta acta) {
    if (acta.requiereCompromisos() && acta.getCompromisos().isEmpty()) {
      return;
    }
    if (!procesoService.resultadoHabilitado(
        acta.getDocente().getIdUsuario(), acta.getPeriodo().getIdPeriodo())) {
      return;
    }
    eventos.publishEvent(
        EventoNotificacion.aUsuario(
            TipoNotificacion.ACTA_POR_FIRMAR,
            acta.getDocente().getIdUsuario(),
            ENLACE_ACTA,
            ENTIDAD_ACTA,
            acta.getIdActa(),
            "acta:" + acta.getIdActa()));
  }

  private void avisarFirma(Acta acta, UsuarioSesion sesion) {
    String clave = "acta:" + acta.getIdActa();
    if (sesion.esDocente()) {
      eventos.publishEvent(
          EventoNotificacion.aCoordinadores(
              TipoNotificacion.ACTA_FIRMADA_DOCENTE,
              acta.getDocente(),
              "/docentes/" + acta.getDocente().getIdUsuario(),
              ENTIDAD_ACTA,
              acta.getIdActa(),
              clave));
    }
    if (acta.getEstado() == EstadoActa.FIRMADA) {
      eventos.publishEvent(
          EventoNotificacion.aUsuario(
                  TipoNotificacion.ACTA_FIRMADA,
                  acta.getDocente().getIdUsuario(),
                  ENLACE_ACTA,
                  ENTIDAD_ACTA,
                  acta.getIdActa(),
                  clave)
              .conDetalle("Código de verificación: " + acta.getCodigoVerificacion() + "."));
    }
  }
}
