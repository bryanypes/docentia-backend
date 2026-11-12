package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.dto.DocenteResponse;
import com.uniautonoma.docentia.dto.PanoramaAnalisisResponse;
import com.uniautonoma.docentia.dto.PanoramaAnalisisResponse.DocenteAnalisis;
import com.uniautonoma.docentia.dto.PanoramaAnalisisResponse.Totales;
import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.entity.CitaHallazgo;
import com.uniautonoma.docentia.entity.EstadoHallazgo;
import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import com.uniautonoma.docentia.entity.Hallazgo;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.entity.TipoHallazgo;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.DocenteService;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Panorama del análisis de comentarios para coordinación y vicerrectoría: qué docentes tienen
 * comentarios, cuáles ya se analizaron o publicaron y cuántas fortalezas, oportunidades y citas
 * verificadas salieron. Solo cuenta: no expone comentarios ni textos de hallazgos.
 */
@Service
@Transactional(readOnly = true)
public class PanoramaAnalisisService {

  private static final Set<EstadoSolicitudAnalisis> EN_CURSO =
      EnumSet.of(EstadoSolicitudAnalisis.EN_COLA, EstadoSolicitudAnalisis.EN_PROCESO);

  private final SolicitudAnalisisRepository solicitudRepository;
  private final ComentarioEstudianteRepository comentarioRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final PeriodoAcademicoService periodoService;
  private final DocenteService docenteService;

  public PanoramaAnalisisService(
      SolicitudAnalisisRepository solicitudRepository,
      ComentarioEstudianteRepository comentarioRepository,
      PeriodoAcademicoRepository periodoRepository,
      PeriodoAcademicoService periodoService,
      DocenteService docenteService) {
    this.solicitudRepository = solicitudRepository;
    this.comentarioRepository = comentarioRepository;
    this.periodoRepository = periodoRepository;
    this.periodoService = periodoService;
    this.docenteService = docenteService;
  }

  /**
   * @param periodoId periodo consultado; nulo para el activo
   * @param idFacultad facultad por la que filtra el vicerrector; se ignora para el coordinador
   */
  public PanoramaAnalisisResponse panorama(Long periodoId, Long idFacultad, UsuarioSesion sesion) {
    PeriodoAcademico periodo =
        periodoId == null
            ? periodoService.exigirPeriodoActivo()
            : periodoRepository
                .findById(periodoId)
                .orElseThrow(
                    () -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
    List<DocenteResponse> docentes = docenteService.listarDocentes(idFacultad, sesion);
    List<Long> ids = docentes.stream().map(DocenteResponse::idDocente).toList();
    Map<Long, SolicitudAnalisis> ultimas =
        ids.isEmpty()
            ? Map.of()
            : solicitudRepository.buscarDePeriodo(periodo.getIdPeriodo(), ids).stream()
                .collect(
                    Collectors.toMap(
                        s -> s.getDocente().getIdUsuario(),
                        s -> s,
                        (masNueva, anterior) -> masNueva));

    List<DocenteAnalisis> filas =
        docentes.stream()
            .map(
                docente ->
                    describir(
                        docente,
                        comentarioRepository.countByDocenteIdUsuarioAndPeriodoIdPeriodo(
                            docente.idDocente(), periodo.getIdPeriodo()),
                        Optional.ofNullable(ultimas.get(docente.idDocente()))))
            .toList();
    return new PanoramaAnalisisResponse(PeriodoResumenResponse.desde(periodo), sumar(filas), filas);
  }

  private DocenteAnalisis describir(
      DocenteResponse docente, long comentarios, Optional<SolicitudAnalisis> solicitud) {
    if (solicitud.isEmpty()) {
      return new DocenteAnalisis(
          docente, comentarios, null, null, false, null, null, 0, 0, 0, 0, 0, 0, null);
    }
    SolicitudAnalisis s = solicitud.get();
    List<Hallazgo> hallazgos =
        s.getEstado() == EstadoSolicitudAnalisis.COMPLETADO ? s.getHallazgos() : List.of();
    Predicate<Hallazgo> vigente = h -> h.getEstado() != EstadoHallazgo.DESCARTADO;
    List<CitaHallazgo> citas = hallazgos.stream().flatMap(h -> h.getCitas().stream()).toList();
    return new DocenteAnalisis(
        docente,
        comentarios,
        s.getIdSolicitud(),
        s.getEstado(),
        s.esVisibleParaDocente(),
        s.getComentariosEnviados(),
        s.getComentariosFiltrados(),
        (int)
            hallazgos.stream()
                .filter(vigente)
                .filter(h -> h.getTipo() == TipoHallazgo.FORTALEZA)
                .count(),
        (int)
            hallazgos.stream()
                .filter(vigente)
                .filter(h -> h.getTipo() == TipoHallazgo.OPORTUNIDAD_DE_MEJORA)
                .count(),
        (int) hallazgos.stream().filter(h -> h.getEstado() == EstadoHallazgo.DESCARTADO).count(),
        (int)
            hallazgos.stream()
                .filter(
                    h ->
                        h.getEstado() == EstadoHallazgo.PENDIENTE_VALIDACION
                            || h.getEstado() == EstadoHallazgo.SIN_CITA_VERIFICADA)
                .count(),
        citas.size(),
        (int) citas.stream().filter(CitaHallazgo::isVerificada).count(),
        s.getFechaFin() != null ? s.getFechaFin() : s.getFechaSolicitud());
  }

  private Totales sumar(List<DocenteAnalisis> filas) {
    ToIntFunction<Predicate<DocenteAnalisis>> contar =
        condicion -> (int) filas.stream().filter(condicion).count();
    return new Totales(
        filas.size(),
        contar.applyAsInt(f -> f.comentarios() > 0),
        filas.stream().mapToLong(DocenteAnalisis::comentarios).sum(),
        contar.applyAsInt(f -> f.estado() == EstadoSolicitudAnalisis.COMPLETADO),
        contar.applyAsInt(DocenteAnalisis::publicado),
        contar.applyAsInt(f -> f.estado() == EstadoSolicitudAnalisis.COMPLETADO && !f.publicado()),
        contar.applyAsInt(f -> f.estado() != null && EN_CURSO.contains(f.estado())),
        contar.applyAsInt(f -> f.estado() == EstadoSolicitudAnalisis.ERROR),
        contar.applyAsInt(f -> f.estado() == EstadoSolicitudAnalisis.INSUFICIENTE),
        filas.stream()
            .mapToLong(f -> f.comentariosEnviados() == null ? 0 : f.comentariosEnviados())
            .sum(),
        filas.stream()
            .mapToLong(f -> f.comentariosFiltrados() == null ? 0 : f.comentariosFiltrados())
            .sum(),
        filas.stream().mapToInt(DocenteAnalisis::fortalezas).sum(),
        filas.stream().mapToInt(DocenteAnalisis::oportunidades).sum(),
        filas.stream().mapToInt(DocenteAnalisis::descartados).sum(),
        filas.stream().mapToInt(DocenteAnalisis::porDecidir).sum(),
        filas.stream().mapToInt(DocenteAnalisis::citas).sum(),
        filas.stream().mapToInt(DocenteAnalisis::citasVerificadas).sum());
  }
}
