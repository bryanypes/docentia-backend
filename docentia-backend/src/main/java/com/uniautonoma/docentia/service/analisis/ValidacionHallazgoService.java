package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.dto.ResultadoAnalisisPropioResponse;
import com.uniautonoma.docentia.dto.ResultadoAnalisisPropioResponse.HallazgoPublicado;
import com.uniautonoma.docentia.dto.SolicitudAnalisisResponse;
import com.uniautonoma.docentia.dto.SolicitudAnalisisResponse.HallazgoBorrador;
import com.uniautonoma.docentia.dto.VersionHallazgoResponse;
import com.uniautonoma.docentia.entity.EstadoHallazgo;
import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import com.uniautonoma.docentia.entity.Hallazgo;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.entity.VersionHallazgo;
import com.uniautonoma.docentia.entity.VersionHallazgo.Accion;
import com.uniautonoma.docentia.exception.EtapaCerradaException;
import com.uniautonoma.docentia.exception.NoProcesableException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.HallazgoRepository;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.repository.VersionHallazgoRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.DocenteService;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import com.uniautonoma.docentia.service.VentanaEtapaService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revisión del borrador por la coordinación (E3 y E4): incluir, descartar o editar cada hallazgo y
 * publicar el resultado. Un hallazgo sin cita verificada no se puede incluir, cada cambio deja una
 * versión y el docente solo ve lo incluido en una revisión publicada.
 */
@Service
@Transactional
public class ValidacionHallazgoService {

  private final HallazgoRepository hallazgoRepository;
  private final VersionHallazgoRepository versionRepository;
  private final SolicitudAnalisisRepository solicitudRepository;
  private final UsuarioRepository usuarioRepository;
  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;
  private final VentanaEtapaService ventanaEtapaService;

  public ValidacionHallazgoService(
      HallazgoRepository hallazgoRepository,
      VersionHallazgoRepository versionRepository,
      SolicitudAnalisisRepository solicitudRepository,
      UsuarioRepository usuarioRepository,
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      VentanaEtapaService ventanaEtapaService) {
    this.hallazgoRepository = hallazgoRepository;
    this.versionRepository = versionRepository;
    this.solicitudRepository = solicitudRepository;
    this.usuarioRepository = usuarioRepository;
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.ventanaEtapaService = ventanaEtapaService;
  }

  /**
   * Marca un hallazgo como válido (lo incluye en el resultado).
   *
   * @throws RecursoNoEncontradoException si no existe o su docente está fuera del alcance
   * @throws EtapaCerradaException si la revisión de hallazgos del periodo está cerrada
   * @throws NoProcesableException si el hallazgo no tiene ninguna cita verificada
   */
  public HallazgoBorrador validar(Long idHallazgo, UsuarioSesion sesion) {
    Hallazgo hallazgo = buscarEditable(idHallazgo, sesion);
    if (!hallazgo.tieneCitaVerificada()) {
      throw new NoProcesableException(
          "El hallazgo no tiene ninguna cita que aparezca en los comentarios de los estudiantes;"
              + " no se puede marcar como válido");
    }
    Usuario usuario = usuarioRepository.getReferenceById(sesion.idUsuario());
    if (hallazgo.getEstado() != EstadoHallazgo.VALIDADO) {
      guardarGeneradoSiEsElPrimerCambio(hallazgo);
      hallazgo.validar(usuario);
      registrar(hallazgo, Accion.INCLUIDO, usuario, null);
    }
    return HallazgoBorrador.desde(hallazgoRepository.saveAndFlush(hallazgo));
  }

  /** Excluye un hallazgo del resultado. */
  public HallazgoBorrador descartar(Long idHallazgo, String motivo, UsuarioSesion sesion) {
    Hallazgo hallazgo = buscarEditable(idHallazgo, sesion);
    if (hallazgo.getEstado() != EstadoHallazgo.DESCARTADO) {
      guardarGeneradoSiEsElPrimerCambio(hallazgo);
      hallazgo.descartar();
      registrar(
          hallazgo,
          Accion.DESCARTADO,
          usuarioRepository.getReferenceById(sesion.idUsuario()),
          motivo);
    }
    return HallazgoBorrador.desde(hallazgoRepository.saveAndFlush(hallazgo));
  }

  /** Corrige la redacción de un hallazgo; las citas no cambian. */
  public HallazgoBorrador editar(Long idHallazgo, String texto, UsuarioSesion sesion) {
    Hallazgo hallazgo = buscarEditable(idHallazgo, sesion);
    guardarGeneradoSiEsElPrimerCambio(hallazgo);
    hallazgo.editarTexto(texto.trim());
    registrar(
        hallazgo, Accion.EDITADO, usuarioRepository.getReferenceById(sesion.idUsuario()), null);
    return HallazgoBorrador.desde(hallazgoRepository.saveAndFlush(hallazgo));
  }

  /** Historial de un hallazgo. */
  @Transactional(readOnly = true)
  public List<VersionHallazgoResponse> versiones(Long idHallazgo, UsuarioSesion sesion) {
    buscarVisible(idHallazgo, sesion);
    return versionRepository.findByHallazgoIdHallazgoOrderByVersionAsc(idHallazgo).stream()
        .map(VersionHallazgoResponse::desde)
        .toList();
  }

  /**
   * Publica la revisión: desde aquí el docente ve los hallazgos incluidos y ya no cambian.
   *
   * @throws ReglaNegocioException si quedan hallazgos sin decidir o no se incluyó ninguno
   */
  public SolicitudAnalisisResponse publicar(Long idSolicitud, UsuarioSesion sesion) {
    SolicitudAnalisis solicitud =
        solicitudRepository
            .findById(idSolicitud)
            .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
    exigirAlcance(solicitud, sesion, "Solicitud no encontrada");
    ventanaEtapaService.exigirEtapaAbierta(solicitud.getPeriodo(), TipoEtapa.REVISION_HALLAZGOS);
    if (solicitud.getEstado() != EstadoSolicitudAnalisis.COMPLETADO) {
      throw new ReglaNegocioException("La solicitud no tiene un borrador para publicar");
    }
    boolean quedanPendientes =
        solicitud.getHallazgos().stream()
            .anyMatch(
                hallazgo ->
                    hallazgo.getEstado() == EstadoHallazgo.PENDIENTE_VALIDACION
                        || hallazgo.getEstado() == EstadoHallazgo.SIN_CITA_VERIFICADA);
    if (quedanPendientes) {
      throw new ReglaNegocioException("Incluye o descarta todos los hallazgos antes de publicar");
    }
    if (solicitud.getHallazgos().stream()
        .noneMatch(hallazgo -> hallazgo.getEstado() == EstadoHallazgo.VALIDADO)) {
      throw new ReglaNegocioException("Incluye al menos un hallazgo antes de publicar");
    }
    solicitud.publicarBorrador();
    return SolicitudAnalisisResponse.desde(solicitudRepository.saveAndFlush(solicitud));
  }

  /** Hallazgos incluidos de la última revisión publicada del docente en el periodo activo. */
  @Transactional(readOnly = true)
  public ResultadoAnalisisPropioResponse resultadoPropio(UsuarioSesion sesion) {
    Optional<PeriodoAcademico> activo = periodoService.buscarPeriodoActivo();
    Optional<SolicitudAnalisis> publicada =
        activo
            .flatMap(
                periodo ->
                    solicitudRepository
                        .findFirstByDocenteIdUsuarioAndPeriodoIdPeriodoAndEstadoBorradorOrderByIdSolicitudDesc(
                            sesion.idUsuario(), periodo.getIdPeriodo(), "VALIDADO"))
            .filter(SolicitudAnalisis::esVisibleParaDocente);
    return publicada
        .map(
            solicitud ->
                new ResultadoAnalisisPropioResponse(
                    PeriodoResumenResponse.desde(solicitud.getPeriodo()),
                    solicitud.getHallazgos().stream()
                        .filter(hallazgo -> hallazgo.getEstado() == EstadoHallazgo.VALIDADO)
                        .map(
                            hallazgo ->
                                new HallazgoPublicado(
                                    hallazgo.getTipo().name(), hallazgo.getTexto()))
                        .toList()))
        .orElseGet(() -> new ResultadoAnalisisPropioResponse(null, List.of()));
  }

  private Hallazgo buscarEditable(Long idHallazgo, UsuarioSesion sesion) {
    Hallazgo hallazgo = buscarVisible(idHallazgo, sesion);
    ventanaEtapaService.exigirEtapaAbierta(
        hallazgo.getSolicitud().getPeriodo(), TipoEtapa.REVISION_HALLAZGOS);
    if ("VALIDADO".equals(hallazgo.getSolicitud().getEstadoBorrador())) {
      throw new ReglaNegocioException("La revisión ya se publicó y no cambia");
    }
    return hallazgo;
  }

  private Hallazgo buscarVisible(Long idHallazgo, UsuarioSesion sesion) {
    Hallazgo hallazgo =
        hallazgoRepository
            .findById(idHallazgo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Hallazgo no encontrado"));
    exigirAlcance(hallazgo.getSolicitud(), sesion, "Hallazgo no encontrado");
    return hallazgo;
  }

  private void exigirAlcance(SolicitudAnalisis solicitud, UsuarioSesion sesion, String mensaje) {
    try {
      docenteService.buscarDocenteVisible(solicitud.getDocente().getIdUsuario(), sesion);
    } catch (RecursoNoEncontradoException fueraDeAlcance) {
      throw new RecursoNoEncontradoException(mensaje);
    }
  }

  private void guardarGeneradoSiEsElPrimerCambio(Hallazgo hallazgo) {
    if (versionRepository.countByHallazgoIdHallazgo(hallazgo.getIdHallazgo()) == 0) {
      registrar(hallazgo, Accion.GENERADO, null, null);
    }
  }

  private void registrar(Hallazgo hallazgo, Accion accion, Usuario usuario, String motivo) {
    int siguiente = versionRepository.countByHallazgoIdHallazgo(hallazgo.getIdHallazgo()) + 1;
    versionRepository.saveAndFlush(
        new VersionHallazgo(hallazgo, siguiente, accion, usuario, motivo));
  }
}
