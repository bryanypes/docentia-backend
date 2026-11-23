package com.uniautonoma.docentia.service.formulario;

import com.uniautonoma.docentia.dto.DecisionReaperturaRequest;
import com.uniautonoma.docentia.dto.FormularioEnviadoResponse;
import com.uniautonoma.docentia.dto.SolicitudReaperturaRequest;
import com.uniautonoma.docentia.dto.SolicitudReaperturaResponse;
import com.uniautonoma.docentia.entity.AccionAuditoria;
import com.uniautonoma.docentia.entity.EstadoActa;
import com.uniautonoma.docentia.entity.EstadoSolicitudReapertura;
import com.uniautonoma.docentia.entity.EvaluacionFormulario;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.SolicitudReapertura;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.EvaluacionFormularioRepository;
import com.uniautonoma.docentia.repository.ProcesoEvaluacionRepository;
import com.uniautonoma.docentia.repository.SolicitudReaperturaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.AuditoriaService;
import com.uniautonoma.docentia.service.DocenteService;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrección de formularios ya enviados, como un ticket: la pide quien envió el formulario, la
 * verifica su superior y la atiende la administración reabriendo el formulario.
 *
 * <ul>
 *   <li>Autoevaluación de un docente: verifica la coordinación de su facultad.
 *   <li>Autoevaluación de un coordinador: verifica la vicerrectoría.
 *   <li>Coevaluación de un coordinador a un docente: verifica la vicerrectoría.
 *   <li>Coevaluación de la vicerrectoría a un coordinador: no hay superior, va directo a la
 *       administración.
 * </ul>
 */
@Service
@Transactional
public class ReaperturaFormularioService {

  private static final String ENLACE = "/solicitudes-correccion";
  private static final String ENTIDAD = "SOLICITUD_REAPERTURA";
  private static final EnumSet<EstadoSolicitudReapertura> ABIERTAS =
      EnumSet.of(
          EstadoSolicitudReapertura.PENDIENTE_APROBACION, EstadoSolicitudReapertura.APROBADA);

  private final SolicitudReaperturaRepository solicitudRepository;
  private final EvaluacionFormularioRepository formularioRepository;
  private final UsuarioRepository usuarioRepository;
  private final ProcesoEvaluacionRepository procesoRepository;
  private final PeriodoAcademicoService periodoService;
  private final DocenteService docenteService;
  private final AuditoriaService auditoria;
  private final ApplicationEventPublisher eventos;
  private final Clock reloj;

  public ReaperturaFormularioService(
      SolicitudReaperturaRepository solicitudRepository,
      EvaluacionFormularioRepository formularioRepository,
      UsuarioRepository usuarioRepository,
      ProcesoEvaluacionRepository procesoRepository,
      PeriodoAcademicoService periodoService,
      DocenteService docenteService,
      AuditoriaService auditoria,
      ApplicationEventPublisher eventos,
      Clock reloj) {
    this.solicitudRepository = solicitudRepository;
    this.formularioRepository = formularioRepository;
    this.usuarioRepository = usuarioRepository;
    this.procesoRepository = procesoRepository;
    this.periodoService = periodoService;
    this.docenteService = docenteService;
    this.auditoria = auditoria;
    this.eventos = eventos;
    this.reloj = reloj;
  }

  /**
   * Abre un ticket para corregir un formulario enviado del periodo activo.
   *
   * @throws ReglaNegocioException si el formulario no se ha enviado, ya tiene una solicitud abierta
   *     o el acta del periodo ya está firmada
   */
  public SolicitudReaperturaResponse solicitar(
      SolicitudReaperturaRequest pedido, UsuarioSesion sesion) {
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    Usuario solicitante = usuarioRepository.findById(sesion.idUsuario()).orElseThrow();
    EvaluacionFormulario formulario;
    Rol aprobador;
    if (pedido.tipo() == TipoFormulario.AUTO) {
      if (!sesion.rol().esEvaluable()) {
        throw new AccessDeniedException("Este rol no tiene autoevaluación");
      }
      formulario = enviado(periodo, sesion.idUsuario(), TipoFormulario.AUTO);
      aprobador = sesion.rol().evaluador();
    } else if (pedido.tipo() == TipoFormulario.COEVAL) {
      if (pedido.idDocente() == null) {
        throw new DatosInvalidosException("Indica el docente de la coevaluación", "idDocente");
      }
      Usuario docente = docenteService.buscarEvaluadoDe(pedido.idDocente(), sesion);
      formulario = enviado(periodo, docente.getIdUsuario(), TipoFormulario.COEVAL);
      if (!formulario.getEvaluador().getIdUsuario().equals(sesion.idUsuario())) {
        throw new RecursoNoEncontradoException("Formulario no encontrado");
      }
      // La coordinación responde ante la vicerrectoría; la vicerrectoría no tiene superior.
      aprobador = sesion.esCoordinador() ? Rol.VICERRECTOR : null;
    } else {
      throw new DatosInvalidosException(
          "Solo se corrigen autoevaluaciones y coevaluaciones", "tipo");
    }
    Long idDocente = formulario.getDocente().getIdUsuario();
    if (procesoRepository.existeActaEnEstado(
        idDocente, periodo.getIdPeriodo(), EstadoActa.FIRMADA)) {
      throw new ReglaNegocioException(
          "El acta de este periodo ya está firmada: la evaluación no se puede corregir");
    }
    if (solicitudRepository.existsByFormularioIdAndEstadoIn(formulario.getId(), ABIERTAS)) {
      throw new ReglaNegocioException(
          "Ya hay una solicitud de corrección abierta para este formulario");
    }
    SolicitudReapertura solicitud;
    try {
      solicitud =
          solicitudRepository.saveAndFlush(
              new SolicitudReapertura(
                  formulario, solicitante, pedido.motivo().strip(), aprobador, ahora()));
    } catch (DataIntegrityViolationException choque) {
      throw new ReglaNegocioException(
          "Ya hay una solicitud de corrección abierta para este formulario");
    }
    auditoria.registrar(AccionAuditoria.SOLICITUD_REAPERTURA, ENTIDAD, solicitud.getId(), sesion);
    String clave = "reapertura:" + solicitud.getId();
    if (aprobador == null) {
      eventos.publishEvent(
          EventoNotificacion.aAdministradores(
              TipoNotificacion.REAPERTURA_POR_ATENDER, ENLACE, ENTIDAD, solicitud.getId(), clave));
    } else if (aprobador == Rol.VICERRECTOR) {
      eventos.publishEvent(
          EventoNotificacion.aVicerrectores(
              TipoNotificacion.REAPERTURA_SOLICITADA, ENLACE, ENTIDAD, solicitud.getId(), clave));
    } else {
      eventos.publishEvent(
          EventoNotificacion.aEvaluadores(
              TipoNotificacion.REAPERTURA_SOLICITADA,
              formulario.getDocente(),
              ENLACE,
              ENTIDAD,
              solicitud.getId(),
              clave));
    }
    return SolicitudReaperturaResponse.desde(solicitud);
  }

  /** Las solicitudes que hizo el usuario, la más reciente primero. */
  @Transactional(readOnly = true)
  public List<SolicitudReaperturaResponse> listarPropias(UsuarioSesion sesion) {
    return solicitudRepository
        .findBySolicitanteIdUsuarioOrderByFechaSolicitudDesc(sesion.idUsuario())
        .stream()
        .map(SolicitudReaperturaResponse::desde)
        .toList();
  }

  /**
   * Las que le toca verificar: la coordinación, las de su facultad; la vicerrectoría, las suyas.
   */
  @Transactional(readOnly = true)
  public List<SolicitudReaperturaResponse> listarPorVerificar(UsuarioSesion sesion) {
    return solicitudRepository.findByRolAprobadorOrderByFechaSolicitudDesc(sesion.rol()).stream()
        .filter(solicitud -> enAlcance(solicitud, sesion))
        .map(SolicitudReaperturaResponse::desde)
        .toList();
  }

  /** Todas, para la administración. No llevan notas ni respuestas. */
  @Transactional(readOnly = true)
  public List<SolicitudReaperturaResponse> listarTodas() {
    return solicitudRepository.findAllByOrderByFechaSolicitudDesc().stream()
        .map(SolicitudReaperturaResponse::desde)
        .toList();
  }

  /**
   * Verificación del superior.
   *
   * @throws RecursoNoEncontradoException si no le corresponde verificarla
   */
  public SolicitudReaperturaResponse verificar(
      Long idSolicitud, DecisionReaperturaRequest decision, UsuarioSesion sesion) {
    SolicitudReapertura solicitud = buscar(idSolicitud);
    if (solicitud.getRolAprobador() != sesion.rol() || !enAlcance(solicitud, sesion)) {
      throw new RecursoNoEncontradoException("Solicitud no encontrada");
    }
    boolean aprobada = decision.aprobada();
    String respuesta = exigirRespuestaSiRechaza(aprobada, decision.respuesta());
    solicitud.decidirAprobacion(
        aprobada, usuarioRepository.getReferenceById(sesion.idUsuario()), respuesta, ahora());
    String clave = "reapertura:" + solicitud.getId();
    avisarSolicitante(
        solicitud,
        aprobada ? TipoNotificacion.REAPERTURA_APROBADA : TipoNotificacion.REAPERTURA_RECHAZADA,
        ENLACE,
        clave + (aprobada ? ":aprobada" : ":rechazada"));
    if (aprobada) {
      eventos.publishEvent(
          EventoNotificacion.aAdministradores(
              TipoNotificacion.REAPERTURA_POR_ATENDER, ENLACE, ENTIDAD, solicitud.getId(), clave));
    }
    return SolicitudReaperturaResponse.desde(solicitud);
  }

  /**
   * La administración la atiende: reabre el formulario para que se corrija, o la rechaza.
   *
   * @throws ReglaNegocioException si aún no tiene la verificación del superior o ya se atendió
   */
  public SolicitudReaperturaResponse atender(
      Long idSolicitud, DecisionReaperturaRequest decision, UsuarioSesion sesion) {
    SolicitudReapertura solicitud = buscar(idSolicitud);
    boolean reabrir = decision.aprobada();
    String respuesta = exigirRespuestaSiRechaza(reabrir, decision.respuesta());
    solicitud.atender(
        reabrir, usuarioRepository.getReferenceById(sesion.idUsuario()), respuesta, ahora());
    String clave = "reapertura:" + solicitud.getId();
    if (reabrir) {
      EvaluacionFormulario formulario = solicitud.getFormulario();
      formulario.reabrir();
      formularioRepository.save(formulario);
      auditoria.registrar(
          AccionAuditoria.REAPERTURA_FORMULARIO, "FORMULARIO", formulario.getId(), sesion);
      avisarSolicitante(
          solicitud, TipoNotificacion.REAPERTURA_ATENDIDA, enlaceDelFormulario(formulario), clave);
    } else {
      avisarSolicitante(
          solicitud, TipoNotificacion.REAPERTURA_RECHAZADA, ENLACE, clave + ":rechazada");
    }
    return SolicitudReaperturaResponse.desde(solicitud);
  }

  /**
   * Autoevaluación y coevaluación de un docente en el periodo activo, para que la administración
   * elija cuál reabrir. Sin puntajes.
   */
  @Transactional(readOnly = true)
  public List<FormularioEnviadoResponse> formulariosDe(Long idDocente) {
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    Usuario docente =
        usuarioRepository
            .findById(idDocente)
            .filter(u -> u.getRol().esEvaluable())
            .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado"));
    boolean actaFirmada =
        procesoRepository.existeActaEnEstado(
            docente.getIdUsuario(), periodo.getIdPeriodo(), EstadoActa.FIRMADA);
    return List.of(TipoFormulario.AUTO, TipoFormulario.COEVAL).stream()
        .flatMap(
            tipo ->
                formularioRepository
                    .findByPeriodoIdPeriodoAndDocenteIdUsuarioAndTipo(
                        periodo.getIdPeriodo(), docente.getIdUsuario(), tipo)
                    .stream())
        .map(formulario -> describir(formulario, actaFirmada))
        .toList();
  }

  /**
   * La administración reabre un formulario enviado sin que nadie lo haya pedido, para que quien lo
   * respondió lo vuelva a hacer. Queda como una solicitud ya atendida y en la auditoría.
   *
   * @throws ReglaNegocioException si no está enviado, el acta ya está firmada o hay una solicitud
   *     abierta para el mismo formulario
   */
  public SolicitudReaperturaResponse reabrirDirecto(
      Long idFormulario, String motivo, UsuarioSesion sesion) {
    EvaluacionFormulario formulario =
        formularioRepository
            .findById(idFormulario)
            .orElseThrow(() -> new RecursoNoEncontradoException("Formulario no encontrado"));
    String noReabrible = motivoNoReabrible(formulario);
    if (noReabrible != null) {
      throw new ReglaNegocioException(noReabrible);
    }
    Usuario administrador = usuarioRepository.getReferenceById(sesion.idUsuario());
    SolicitudReapertura solicitud;
    try {
      // Sin aprobador: nace aprobada y la administración la atiende de inmediato.
      solicitud =
          solicitudRepository.saveAndFlush(
              new SolicitudReapertura(formulario, administrador, motivo.strip(), null, ahora()));
    } catch (DataIntegrityViolationException choque) {
      throw new ReglaNegocioException(
          "Ya hay una solicitud de corrección abierta para este formulario: atiéndela");
    }
    solicitud.atender(true, administrador, null, ahora());
    formulario.reabrir();
    formularioRepository.save(formulario);
    auditoria.registrar(
        AccionAuditoria.REAPERTURA_FORMULARIO, "FORMULARIO", formulario.getId(), sesion);
    Usuario quienResponde =
        formulario.getEvaluador() != null ? formulario.getEvaluador() : formulario.getDocente();
    eventos.publishEvent(
        EventoNotificacion.aUsuario(
            TipoNotificacion.REAPERTURA_ATENDIDA,
            quienResponde.getIdUsuario(),
            enlaceDelFormulario(formulario),
            ENTIDAD,
            solicitud.getId(),
            "reapertura:" + solicitud.getId()));
    return SolicitudReaperturaResponse.desde(solicitud);
  }

  private FormularioEnviadoResponse describir(
      EvaluacionFormulario formulario, boolean actaFirmada) {
    String motivo =
        actaFirmada ? "El acta de este periodo ya está firmada" : motivoNoReabrible(formulario);
    Usuario evaluador =
        formulario.getEvaluador() != null ? formulario.getEvaluador() : formulario.getDocente();
    return new FormularioEnviadoResponse(
        formulario.getId(),
        formulario.getTipo(),
        evaluador.getNombreCompleto(),
        formulario.getEstado().name(),
        formulario.getEnviadaEn(),
        motivo == null,
        motivo);
  }

  /** Por qué no se puede reabrir, o nulo si se puede. */
  private String motivoNoReabrible(EvaluacionFormulario formulario) {
    if (!formulario.estaEnviada()) {
      return formulario.getReabierto()
          ? "Ya está reabierto: falta que lo envíen de nuevo"
          : "Todavía no se ha enviado";
    }
    if (procesoRepository.existeActaEnEstado(
        formulario.getDocente().getIdUsuario(),
        formulario.getPeriodo().getIdPeriodo(),
        EstadoActa.FIRMADA)) {
      return "El acta de este periodo ya está firmada";
    }
    if (solicitudRepository.existsByFormularioIdAndEstadoIn(formulario.getId(), ABIERTAS)) {
      return "Tiene una solicitud de corrección abierta: atiéndela en la lista";
    }
    return null;
  }

  private EvaluacionFormulario enviado(
      PeriodoAcademico periodo, Long idDocente, TipoFormulario tipo) {
    EvaluacionFormulario formulario =
        formularioRepository
            .findByPeriodoIdPeriodoAndDocenteIdUsuarioAndTipo(
                periodo.getIdPeriodo(), idDocente, tipo)
            .orElseThrow(
                () ->
                    new ReglaNegocioException(
                        "Solo se puede pedir la corrección de un formulario ya enviado"));
    if (!formulario.estaEnviada()) {
      throw new ReglaNegocioException(
          formulario.getReabierto()
              ? "El formulario ya está reabierto: corrígelo y envíalo"
              : "Solo se puede pedir la corrección de un formulario ya enviado");
    }
    return formulario;
  }

  private boolean enAlcance(SolicitudReapertura solicitud, UsuarioSesion sesion) {
    if (sesion.esVicerrector()) {
      return true;
    }
    Usuario docente = solicitud.getFormulario().getDocente();
    return sesion.esCoordinador()
        && docente.getRol() == Rol.DOCENTE
        && Objects.equals(docente.getFacultadDeAdscripcion().getIdFacultad(), sesion.idFacultad());
  }

  private static String exigirRespuestaSiRechaza(boolean aprueba, String respuesta) {
    String texto = respuesta == null ? null : respuesta.strip();
    if (!aprueba && (texto == null || texto.length() < 10)) {
      throw new DatosInvalidosException(
          "Explica al solicitante por qué se rechaza (al menos 10 caracteres)", "respuesta");
    }
    return texto == null || texto.isEmpty() ? null : texto;
  }

  private static String enlaceDelFormulario(EvaluacionFormulario formulario) {
    return formulario.getTipo() == TipoFormulario.AUTO
        ? "/mi-autoevaluacion"
        : "/coevaluaciones/" + formulario.getDocente().getIdUsuario();
  }

  private void avisarSolicitante(
      SolicitudReapertura solicitud, TipoNotificacion tipo, String enlace, String clave) {
    eventos.publishEvent(
        EventoNotificacion.aUsuario(
            tipo,
            solicitud.getSolicitante().getIdUsuario(),
            enlace,
            ENTIDAD,
            solicitud.getId(),
            clave));
  }

  private SolicitudReapertura buscar(Long idSolicitud) {
    return solicitudRepository
        .findById(idSolicitud)
        .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
  }

  private LocalDateTime ahora() {
    return LocalDateTime.now(reloj);
  }
}
