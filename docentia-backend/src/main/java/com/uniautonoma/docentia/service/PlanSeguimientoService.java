package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.dto.PlanSeguimientoResponse;
import com.uniautonoma.docentia.dto.PlanSeguimientoResponse.CambioEstado;
import com.uniautonoma.docentia.dto.PlanSeguimientoResponse.CompromisoSeguimiento;
import com.uniautonoma.docentia.dto.PlanSeguimientoResponse.EstadoPlan;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.EstadoCompromiso;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.SeguimientoCompromiso;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.CompromisoRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.SeguimientoCompromisoRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seguimiento en el periodo N de los compromisos adquiridos en el periodo N-1. */
@Service
@Transactional
public class PlanSeguimientoService {

  private final CompromisoRepository compromisoRepository;
  private final SeguimientoCompromisoRepository seguimientoRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final UsuarioRepository usuarioRepository;
  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;
  private final Clock reloj;

  public PlanSeguimientoService(
      CompromisoRepository compromisoRepository,
      SeguimientoCompromisoRepository seguimientoRepository,
      PeriodoAcademicoRepository periodoRepository,
      UsuarioRepository usuarioRepository,
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      Clock reloj) {
    this.compromisoRepository = compromisoRepository;
    this.seguimientoRepository = seguimientoRepository;
    this.periodoRepository = periodoRepository;
    this.usuarioRepository = usuarioRepository;
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.reloj = reloj;
  }

  /**
   * Plan de un docente para coordinación o vicerrectoría.
   *
   * @throws RecursoNoEncontradoException si el docente está fuera del alcance
   */
  @Transactional(readOnly = true)
  public PlanSeguimientoResponse consultar(Long idDocente, UsuarioSesion sesion) {
    docenteService.buscarDocenteVisible(idDocente, sesion);
    return armar(idDocente);
  }

  /** Plan del docente autenticado. */
  @Transactional(readOnly = true)
  public PlanSeguimientoResponse consultarPropio(UsuarioSesion sesion) {
    return armar(sesion.idUsuario());
  }

  /**
   * Marca cumplido un compromiso y deja el cambio con su fecha y autor.
   *
   * @throws RecursoNoEncontradoException si no existe o su docente está fuera del alcance
   */
  public PlanSeguimientoResponse marcarCumplido(
      Long idCompromiso, String observacion, UsuarioSesion sesion) {
    Compromiso compromiso =
        compromisoRepository
            .findById(idCompromiso)
            .orElseThrow(() -> new RecursoNoEncontradoException("Compromiso no encontrado"));
    Long idDocente = compromiso.getDocente().getIdUsuario();
    try {
      docenteService.buscarDocenteVisible(idDocente, sesion);
    } catch (RecursoNoEncontradoException fueraDeAlcance) {
      throw new RecursoNoEncontradoException("Compromiso no encontrado");
    }
    String texto = observacion.trim();
    compromiso.marcarCumplido(texto);
    seguimientoRepository.save(
        new SeguimientoCompromiso(
            compromiso,
            EstadoCompromiso.CUMPLIDO,
            texto,
            usuarioRepository.getReferenceById(sesion.idUsuario())));
    seguimientoRepository.flush();
    return armar(idDocente);
  }

  private PlanSeguimientoResponse armar(Long idDocente) {
    Optional<PeriodoAcademico> anterior =
        periodoService
            .buscarPeriodoActivo()
            .flatMap(
                activo ->
                    periodoRepository.findFirstByFechaInicioLessThanOrderByFechaInicioDesc(
                        activo.getFechaInicio()));
    if (anterior.isEmpty()) {
      return new PlanSeguimientoResponse(null, List.of());
    }
    LocalDate hoy = LocalDate.now(reloj);
    return new PlanSeguimientoResponse(
        PeriodoResumenResponse.desde(anterior.get()),
        compromisoRepository.buscarEnFirme(idDocente, anterior.get().getIdPeriodo()).stream()
            .map(compromiso -> describir(compromiso, hoy))
            .toList());
  }

  private CompromisoSeguimiento describir(Compromiso compromiso, LocalDate hoy) {
    return new CompromisoSeguimiento(
        compromiso.getIdCompromiso(),
        compromiso.getDescripcion(),
        compromiso.getFechaLimite(),
        estadoDe(compromiso, hoy),
        compromiso.getObservacion(),
        seguimientoRepository
            .findByCompromisoIdCompromisoOrderByFechaDesc(compromiso.getIdCompromiso())
            .stream()
            .map(this::cambio)
            .toList());
  }

  private CambioEstado cambio(SeguimientoCompromiso cambio) {
    return new CambioEstado(
        cambio.getEstado().name(),
        cambio.getObservacion(),
        cambio.getUsuario().getNombreCompleto(),
        cambio.getFecha());
  }

  private EstadoPlan estadoDe(Compromiso compromiso, LocalDate hoy) {
    if (compromiso.getEstado() == EstadoCompromiso.CUMPLIDO) {
      return EstadoPlan.CUMPLIDO;
    }
    return compromiso.getFechaLimite().isBefore(hoy) ? EstadoPlan.VENCIDO : EstadoPlan.PENDIENTE;
  }
}
