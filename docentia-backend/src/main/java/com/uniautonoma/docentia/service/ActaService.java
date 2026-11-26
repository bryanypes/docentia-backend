package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.config.ActasProperties;
import com.uniautonoma.docentia.dto.ActaPorCodigoResponse;
import com.uniautonoma.docentia.dto.ActaResponse;
import com.uniautonoma.docentia.dto.CompromisoActaRequest;
import com.uniautonoma.docentia.dto.EstadoPromedio;
import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.dto.VerificacionActaResponse;
import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.CorreccionCalificacion;
import com.uniautonoma.docentia.entity.DatosCongeladosActa;
import com.uniautonoma.docentia.entity.EstadoActa;
import com.uniautonoma.docentia.entity.FirmaActa;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.PesosFuentes;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoActa;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.TipoHallazgo;
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
import java.util.Optional;
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
    Usuario docente = docenteService.buscarEvaluadoDe(idDocente, sesion);
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
    // El coordinador consulta las suyas sin indicar docente, igual que un docente.
    boolean propias =
        sesion.esDocente()
            || (sesion.esCoordinador()
                && (idDocente == null || idDocente.equals(sesion.idUsuario())));
    Long id = propias ? sesion.idUsuario() : idDocente;
    if (id == null) {
      throw new DatosInvalidosException("Indica el docente");
    }
    if (!propias) {
      docenteService.buscarDocenteVisible(id, sesion);
    }
    return actaRepository
        .findByDocenteIdUsuarioOrderByPeriodoFechaInicioDescVersionDesc(id)
        .stream()
        .filter(acta -> !propias || visibleParaDocente(acta))
        .map(acta -> propias ? ActaResponse.paraDocente(acta) : ActaResponse.desde(acta))
        .toList();
  }

  @Transactional(readOnly = true)
  public ActaResponse obtener(Long idActa, UsuarioSesion sesion) {
    Acta acta = buscarVisible(idActa, sesion);
    return esPropia(acta, sesion) ? ActaResponse.paraDocente(acta) : ActaResponse.desde(acta);
  }

  /**
   * Cambia el tipo del acta mientras nadie la haya firmado.
   *
   * @throws ReglaNegocioException si ya tiene firmas o la nota exige el acta de compromiso
   */
  public ActaResponse cambiarTipo(Long idActa, TipoActa tipo, UsuarioSesion sesion) {
    Acta acta = buscarComoEvaluador(idActa, sesion);
    acta.cambiarTipo(tipo);
    Acta guardada = actaRepository.saveAndFlush(acta);
    avisarSiEstaLista(guardada);
    return ActaResponse.desde(guardada);
  }

  /** Guarda lo que se habló en la reunión; solo quien evaluó y antes de la primera firma. */
  public ActaResponse guardarObservaciones(Long idActa, String texto, UsuarioSesion sesion) {
    Acta acta = buscarComoEvaluador(idActa, sesion);
    acta.registrarObservaciones(texto);
    return ActaResponse.desde(actaRepository.saveAndFlush(acta));
  }

  /**
   * Agrega un compromiso al acta en borrador.
   *
   * @throws ReglaNegocioException si el acta ya está firmada
   * @throws DatosInvalidosException si el plazo ya pasó
   */
  public ActaResponse agregarCompromiso(
      Long idActa, CompromisoActaRequest solicitud, UsuarioSesion sesion) {
    Acta acta = buscarComoEvaluador(idActa, sesion);
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
    Acta acta = buscarComoEvaluador(idActa, sesion);
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
    Rol papel = papelAlFirmar(acta, sesion);
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
    if (acta.firmadaPor(papel)) {
      throw new ReglaNegocioException("Ya firmaste esta acta");
    }
    congelar(acta);
    // La copia se escribe antes que la firma: con una firma registrada, la base ya no deja
    // cambiar el contenido del acta (V52).
    actaRepository.flush();
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
            acta, sesion.idUsuario(), papel, sesion.nombreCompleto(), sesion.correo(), ip, ahora);
    acta.registrarFirma(png == null ? firma : firma.conTrazo(png, generadorPdf.sha256(png)));
    if (acta.firmadaPorAmbos()) {
      cerrar(acta, ahora);
    }
    Acta guardada = actaRepository.saveAndFlush(acta);
    avisarFirma(guardada, papel);
    return esPropia(guardada, sesion)
        ? ActaResponse.paraDocente(guardada)
        : ActaResponse.desde(guardada);
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

  /**
   * PDF guardado del acta; nunca se regenera. Lleva la nota, así que solo lo descargan quienes ven
   * el acta: el docente, quien lo evalúa y la vicerrectoría. La administración, que no ve notas, se
   * queda con el registro de actas firmadas.
   */
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

  /** Búsqueda por código para la administración y la vicerrectoría: sin notas. */
  @Transactional(readOnly = true)
  public ActaPorCodigoResponse buscarPorCodigo(String codigo) {
    return actaRepository
        .findByCodigoVerificacion(codigo.trim().toUpperCase(Locale.ROOT))
        .map(ActaService::resumenAdministrativo)
        .orElseThrow(() -> new RecursoNoEncontradoException("Ningún acta tiene ese código"));
  }

  /** Registro de todas las actas firmadas, para la administración y la vicerrectoría. */
  @Transactional(readOnly = true)
  public List<ActaPorCodigoResponse> registro() {
    return actaRepository.findByCodigoVerificacionIsNotNullOrderByFechaFirmaDesc().stream()
        .map(ActaService::resumenAdministrativo)
        .toList();
  }

  private static ActaPorCodigoResponse resumenAdministrativo(Acta acta) {
    DatosCongeladosActa datos = acta.getDatosCongelados();
    Usuario docente = acta.getDocente();
    return new ActaPorCodigoResponse(
        acta.getIdActa(),
        acta.getCodigoVerificacion(),
        acta.getTipo() == TipoActa.COMPROMISO
            ? "Acta de compromiso docente"
            : "Acta de retroalimentación docente",
        acta.getEstado(),
        datos.periodoNombre() != null ? datos.periodoNombre() : acta.getPeriodo().getNombre(),
        datos.docenteNombre() != null ? datos.docenteNombre() : docente.getNombreCompleto(),
        datos.docenteCorreo() != null ? datos.docenteCorreo() : docente.getCorreo(),
        datos.docenteNombre() != null
            ? datos.programaNombre()
            : docente.getPrograma() == null ? null : docente.getPrograma().getNombre(),
        datos.docenteNombre() != null
            ? (datos.escuelaNombre() == null ? "No aplica" : datos.escuelaNombre())
            : GeneradorPdfActa.nombreEscuela(docente.getFacultadDeAdscripcion()),
        acta.getFechaFirma(),
        acta.getFirmas().stream()
            .map(
                f ->
                    new ActaPorCodigoResponse.Firmante(
                        f.getNombre(),
                        GeneradorPdfActa.cargoDelFirmante(acta, f.getRol()),
                        f.getCorreo(),
                        f.getFecha()))
            .toList(),
        acta.getHashContenido());
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

  /** Copia el resultado y los datos del acta. Desde la primera firma el acta ya no cambia. */
  private void congelar(Acta acta) {
    if (!acta.getFirmas().isEmpty()) {
      return;
    }
    Long idDocente = acta.getDocente().getIdUsuario();
    PeriodoAcademico periodo = acta.getPeriodo();
    PromedioPonderadoResponse promedio = promedioService.obtenerPromedio(idDocente, periodo);
    EvaluacionPeriodo evaluacion = promedioService.cargarEvaluacion(idDocente, periodo);
    Map<FuenteEvaluacion, BigDecimal> notas = evaluacion.notasDeFuentes();
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
    acta.congelarDatos(datosActuales(acta, evaluacion.pesos()));
  }

  /**
   * Identificación, reunión, pesos y aspectos conversados tal como están hoy. El acta los copia
   * hasta la primera firma; desde ahí imprime y muestra esa copia.
   */
  private DatosCongeladosActa datosActuales(Acta acta, PesosFuentes pesos) {
    Usuario docente = acta.getDocente();
    PeriodoAcademico periodo = acta.getPeriodo();
    Long idDocente = docente.getIdUsuario();
    Long idPeriodo = periodo.getIdPeriodo();
    Optional<Cita> cita = citaRepository.buscarActiva(idDocente, idPeriodo);
    String rotulo = propiedades.actas().rotuloDocumento();
    String documento =
        rotulo == null || docente.getCodigo() == null || docente.getCodigo().isBlank()
            ? null
            : docente.getCodigo().strip();
    long respuestas = actaRepository.contarRespuestasEstudiantes(idDocente, idPeriodo);
    return new DatosCongeladosActa(
        docente.getNombreCompleto(),
        docente.getCorreo(),
        documento == null ? null : rotulo,
        documento,
        GeneradorPdfActa.cargoEnVivo(docente, Rol.DOCENTE),
        docente.getPrograma() == null ? null : docente.getPrograma().getNombre(),
        docente.getFacultadDeAdscripcion() == null
            ? null
            : GeneradorPdfActa.nombreEscuela(docente.getFacultadDeAdscripcion()),
        periodo.getNombre(),
        periodo.getFechaInicio(),
        periodo.getFechaFin(),
        GeneradorPdfActa.cargoEnVivo(docente, acta.rolEvaluador()),
        cita.map(c -> c.getFranja().getFecha()).orElse(null),
        cita.map(c -> c.getFranja().getHoraInicio()).orElse(null),
        cita.map(c -> c.getFranja().getHoraFin()).orElse(null),
        cita.map(Cita::getAsistencia).map(Enum::name).orElse(null),
        pesos.pesoEstudiantes(),
        pesos.pesoCoordinacion(),
        pesos.pesoAutoevaluacion(),
        (int) respuestas,
        generadorPdf.hallazgosValidados(idDocente, idPeriodo, TipoHallazgo.FORTALEZA),
        generadorPdf.hallazgosValidados(idDocente, idPeriodo, TipoHallazgo.OPORTUNIDAD_DE_MEJORA),
        propiedades.actas().formatoCodigo(),
        propiedades.actas().formatoVersion(),
        docente.getFacultadDeAdscripcion() == null
            ? null
            : docente.getFacultadDeAdscripcion().getIdFacultad());
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
    if (esPropia(acta, sesion)) {
      if (!visibleParaDocente(acta)) {
        throw new RecursoNoEncontradoException("Acta no encontrada");
      }
      return acta;
    }
    if (sesion.esDocente()) {
      throw new RecursoNoEncontradoException("Acta no encontrada");
    }
    if (sesion.esCoordinador()) {
      Long facultadCongelada = acta.getDatosCongelados().facultadId();
      // La coordinación ve las actas de los docentes de su escuela, nunca las de otro
      // coordinador: esas las evalúa la vicerrectoría.
      if (acta.getDocente().getRol() != Rol.DOCENTE
          || facultadCongelada == null
          || !facultadCongelada.equals(sesion.idFacultad())) {
        throw new RecursoNoEncontradoException("Acta no encontrada");
      }
    } else if (!sesion.esVicerrector()) {
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

  /** El acta es del propio usuario: un docente, o un coordinador evaluado como docente. */
  private static boolean esPropia(Acta acta, UsuarioSesion sesion) {
    return acta.getDocente().getIdUsuario().equals(sesion.idUsuario());
  }

  /** Acta que el usuario puede editar porque evaluó al docente. */
  private Acta buscarComoEvaluador(Long idActa, UsuarioSesion sesion) {
    Acta acta = buscarVisible(idActa, sesion);
    if (esPropia(acta, sesion) || acta.rolEvaluador() != sesion.rol()) {
      throw new RecursoNoEncontradoException("Acta no encontrada");
    }
    return acta;
  }

  /**
   * Con qué papel firma: el evaluado como DOCENTE y su evaluador con su rol. La vicerrectoría solo
   * firma las actas de los coordinadores; las de los docentes las firma la coordinación.
   */
  private static Rol papelAlFirmar(Acta acta, UsuarioSesion sesion) {
    if (esPropia(acta, sesion)) {
      return Rol.DOCENTE;
    }
    if (acta.rolEvaluador() != sesion.rol()) {
      throw new AccessDeniedException(
          acta.rolEvaluador() == Rol.VICERRECTOR
              ? "Esta acta la firma la vicerrectoría"
              : "Esta acta la firma la coordinación");
    }
    return sesion.rol();
  }

  private void avisarFirma(Acta acta, Rol papel) {
    String clave = "acta:" + acta.getIdActa();
    if (papel == Rol.DOCENTE) {
      eventos.publishEvent(
          EventoNotificacion.aEvaluadores(
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
