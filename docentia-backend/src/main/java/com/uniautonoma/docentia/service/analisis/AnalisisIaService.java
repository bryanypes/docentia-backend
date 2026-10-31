package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.config.IaProperties;
import com.uniautonoma.docentia.dto.ComentarioFiltradoResponse;
import com.uniautonoma.docentia.dto.SolicitudAnalisisRequest;
import com.uniautonoma.docentia.dto.SolicitudAnalisisResponse;
import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.EtapaCerradaException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.exception.ServicioDeshabilitadoException;
import com.uniautonoma.docentia.repository.ComentarioFiltradoRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.DocenteService;
import com.uniautonoma.docentia.service.VentanaEtapaService;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro y consulta de solicitudes de análisis. Registrar es inmediato: el análisis lo hace
 * después el procesador en segundo plano.
 */
@Service
@Transactional
public class AnalisisIaService {

  private static final EnumSet<EstadoSolicitudAnalisis> EN_CURSO =
      EnumSet.of(EstadoSolicitudAnalisis.EN_COLA, EstadoSolicitudAnalisis.EN_PROCESO);

  private final SolicitudAnalisisRepository solicitudRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final UsuarioRepository usuarioRepository;
  private final DocenteService docenteService;
  private final VentanaEtapaService ventanaEtapaService;
  private final IaProperties propiedades;
  private final ComentarioFiltradoRepository filtradoRepository;
  private final ProveedorComentarios proveedorComentarios;
  private final Anonimizador anonimizador;

  public AnalisisIaService(
      SolicitudAnalisisRepository solicitudRepository,
      PeriodoAcademicoRepository periodoRepository,
      UsuarioRepository usuarioRepository,
      DocenteService docenteService,
      VentanaEtapaService ventanaEtapaService,
      IaProperties propiedades,
      ComentarioFiltradoRepository filtradoRepository,
      ProveedorComentarios proveedorComentarios,
      Anonimizador anonimizador) {
    this.filtradoRepository = filtradoRepository;
    this.proveedorComentarios = proveedorComentarios;
    this.anonimizador = anonimizador;
    this.solicitudRepository = solicitudRepository;
    this.periodoRepository = periodoRepository;
    this.usuarioRepository = usuarioRepository;
    this.docenteService = docenteService;
    this.ventanaEtapaService = ventanaEtapaService;
    this.propiedades = propiedades;
  }

  /**
   * Registra una solicitud en la cola.
   *
   * @param solicitud docente y periodo a analizar
   * @param sesion coordinador; solo docentes de su facultad
   * @return la solicitud en estado EN_COLA
   * @throws ServicioDeshabilitadoException si el análisis con IA está apagado en este ambiente
   * @throws RecursoNoEncontradoException si el docente está fuera del alcance o el periodo no
   *     existe
   * @throws EtapaCerradaException si la revisión de hallazgos no está abierta en ese periodo
   * @throws ReglaNegocioException si ya hay una solicitud en curso para el docente y el periodo
   */
  public SolicitudAnalisisResponse solicitar(
      SolicitudAnalisisRequest solicitud, UsuarioSesion sesion) {
    if (!propiedades.habilitado()) {
      throw new ServicioDeshabilitadoException(
          "El análisis de comentarios con IA está deshabilitado en este ambiente");
    }
    Usuario docente = docenteService.buscarDocenteVisible(solicitud.docenteId(), sesion);
    PeriodoAcademico periodo =
        periodoRepository
            .findById(solicitud.periodoId())
            .orElseThrow(() -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
    ventanaEtapaService.exigirEtapaAbierta(periodo, TipoEtapa.REVISION_HALLAZGOS);
    solicitudRepository
        .findFirstByDocenteIdUsuarioAndPeriodoIdPeriodoAndEstadoIn(
            docente.getIdUsuario(), periodo.getIdPeriodo(), EN_CURSO)
        .ifPresent(
            enCurso -> {
              throw new ReglaNegocioException(
                  "Ya hay una solicitud de análisis en curso para este docente y periodo (#"
                      + enCurso.getIdSolicitud()
                      + ")");
            });
    SolicitudAnalisis nueva =
        solicitudRepository.saveAndFlush(
            new SolicitudAnalisis(
                docente, periodo, usuarioRepository.getReferenceById(sesion.idUsuario())));
    return SolicitudAnalisisResponse.desde(nueva);
  }

  /**
   * Consulta una solicitud con su borrador.
   *
   * @throws RecursoNoEncontradoException si no existe o su docente está fuera del alcance
   */
  @Transactional(readOnly = true)
  public SolicitudAnalisisResponse consultar(Long idSolicitud, UsuarioSesion sesion) {
    SolicitudAnalisis solicitud = buscarVisible(idSolicitud, sesion);
    if (solicitud.getEstado() != EstadoSolicitudAnalisis.COMPLETADO) {
      return SolicitudAnalisisResponse.desde(solicitud);
    }
    return SolicitudAnalisisResponse.desde(solicitud, comentariosEnviados(solicitud));
  }

  /**
   * Comentarios del docente y periodo que pasaron el filtro, anonimizados como los recibió el
   * modelo. Solo de esos se muestra el texto: una cita con un id ajeno o de un comentario filtrado
   * no expone nada.
   */
  private Map<Long, String> comentariosEnviados(SolicitudAnalisis solicitud) {
    Set<Long> filtrados =
        new HashSet<>(filtradoRepository.idsFiltrados(solicitud.getIdSolicitud()));
    Map<Long, String> textos = new HashMap<>();
    for (ComentarioFuente comentario :
        proveedorComentarios.comentarios(
            solicitud.getDocente().getIdUsuario(), solicitud.getPeriodo().getIdPeriodo())) {
      if (!filtrados.contains(comentario.idComentario())) {
        textos.put(comentario.idComentario(), anonimizador.anonimizar(comentario.texto()));
      }
    }
    return textos;
  }

  /**
   * Lista los comentarios que el filtro excluyó de una solicitud y por qué.
   *
   * @throws RecursoNoEncontradoException si no existe o su docente está fuera del alcance
   */
  @Transactional(readOnly = true)
  public List<ComentarioFiltradoResponse> listarFiltrados(Long idSolicitud, UsuarioSesion sesion) {
    buscarVisible(idSolicitud, sesion);
    return filtradoRepository.buscarPorSolicitud(idSolicitud).stream()
        .map(ComentarioFiltradoResponse::desde)
        .toList();
  }

  private SolicitudAnalisis buscarVisible(Long idSolicitud, UsuarioSesion sesion) {
    SolicitudAnalisis solicitud =
        solicitudRepository
            .findById(idSolicitud)
            .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
    try {
      docenteService.buscarDocenteVisible(solicitud.getDocente().getIdUsuario(), sesion);
    } catch (RecursoNoEncontradoException fueraDeAlcance) {
      throw new RecursoNoEncontradoException("Solicitud no encontrada");
    }
    return solicitud;
  }

  /**
   * Lista las solicitudes de un docente en un periodo, la más reciente primero.
   *
   * @throws RecursoNoEncontradoException si el docente está fuera del alcance
   */
  @Transactional(readOnly = true)
  public List<SolicitudAnalisisResponse> listar(
      Long idDocente, Long idPeriodo, UsuarioSesion sesion) {
    docenteService.buscarDocenteVisible(idDocente, sesion);
    return solicitudRepository
        .findByDocenteIdUsuarioAndPeriodoIdPeriodoOrderByIdSolicitudDesc(idDocente, idPeriodo)
        .stream()
        .map(SolicitudAnalisisResponse::desde)
        .toList();
  }
}
