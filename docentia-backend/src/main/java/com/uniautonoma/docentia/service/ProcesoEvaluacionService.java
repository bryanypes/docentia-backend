package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.config.HeteroevaluacionProperties;
import com.uniautonoma.docentia.config.ProcesoProperties;
import com.uniautonoma.docentia.dto.DocenteResponse;
import com.uniautonoma.docentia.dto.ProcesoEvaluacionResponse;
import com.uniautonoma.docentia.dto.ProcesoEvaluacionResponse.ActaProceso;
import com.uniautonoma.docentia.dto.ProcesoEvaluacionResponse.CitaProceso;
import com.uniautonoma.docentia.dto.ProcesoEvaluacionResponse.Cobertura;
import com.uniautonoma.docentia.dto.ProcesoEvaluacionResponse.Hito;
import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.AsistenciaCita;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.ConfirmacionCita;
import com.uniautonoma.docentia.entity.EstadoActa;
import com.uniautonoma.docentia.entity.EstadoCita;
import com.uniautonoma.docentia.entity.EstadoProcesoEvaluacion;
import com.uniautonoma.docentia.entity.FirmaActa;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.MotivoLiberacion;
import com.uniautonoma.docentia.entity.OrigenCalificacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoActa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.ProcesoEvaluacionRepository;
import com.uniautonoma.docentia.repository.ProcesoEvaluacionRepository.CoberturaEstudiantes;
import com.uniautonoma.docentia.repository.ProcesoEvaluacionRepository.OrigenNota;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estado general del proceso de evaluación de cada docente: evaluaciones, cita, asistencia y acta.
 * Se deduce cada vez de los datos, así que no hay un estado guardado que se pueda desfasar.
 */
@Service
@Transactional(readOnly = true)
public class ProcesoEvaluacionService {

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

  private final ProcesoEvaluacionRepository procesoRepository;
  private final UsuarioRepository usuarioRepository;
  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;
  private final PromedioService promedioService;
  private final ProcesoProperties propiedades;
  private final HeteroevaluacionProperties heteroevaluacion;

  public ProcesoEvaluacionService(
      ProcesoEvaluacionRepository procesoRepository,
      UsuarioRepository usuarioRepository,
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      PromedioService promedioService,
      ProcesoProperties propiedades,
      HeteroevaluacionProperties heteroevaluacion) {
    this.procesoRepository = procesoRepository;
    this.usuarioRepository = usuarioRepository;
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.promedioService = promedioService;
    this.propiedades = propiedades;
    this.heteroevaluacion = heteroevaluacion;
  }

  /**
   * Proceso de los docentes que el usuario alcanza a ver, en el periodo activo.
   *
   * @param idFacultad facultad por la que filtra el vicerrector; se ignora para el coordinador
   * @return un elemento por docente, en orden por nombre; vacío si no hay periodo activo
   */
  public List<ProcesoEvaluacionResponse> listar(Long idFacultad, UsuarioSesion sesion) {
    Optional<PeriodoAcademico> periodo = periodoService.buscarPeriodoActivo();
    if (periodo.isEmpty()) {
      return List.of();
    }
    return armar(periodo.get(), docenteService.listarDocentes(idFacultad, sesion), true);
  }

  /** Proceso de un docente para coordinación o vicerrectoría. */
  public ProcesoEvaluacionResponse obtener(Long idDocente, UsuarioSesion sesion) {
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    return armar(periodo, List.of(DocenteResponse.desde(docente)), true).get(0);
  }

  /**
   * Proceso del docente autenticado. Mientras estudiantes o coordinación no hayan calificado solo
   * sabe que su evaluación está en curso: sin fuentes, cita ni hitos.
   */
  public ProcesoEvaluacionResponse obtenerPropio(UsuarioSesion sesion) {
    Usuario docente =
        usuarioRepository
            .findByIdUsuarioAndRolAndEsActivoTrue(sesion.idUsuario(), Rol.DOCENTE)
            .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado"));
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    ProcesoEvaluacionResponse completo =
        armar(periodo, List.of(DocenteResponse.desde(docente)), false).get(0);
    if (completo.estado() != EstadoProcesoEvaluacion.EVALUACIONES_EN_CURSO) {
      return completo;
    }
    return new ProcesoEvaluacionResponse(
        completo.docente(),
        completo.estado(),
        completo.estado().getNombre(),
        completo.paso(),
        List.of(),
        null,
        null,
        null,
        false,
        List.of());
  }

  /**
   * El docente ve su nota, sus compromisos del periodo y su acta solo después de que la
   * coordinación registre que asistió a la reunión. Un acta ya firmada también cuenta, para las
   * firmadas antes de esta regla.
   */
  public boolean resultadoHabilitado(Long idDocente, Long idPeriodo) {
    if (!propiedades.exigirAsistencia()) {
      return true;
    }
    return procesoRepository.existsByDocenteIdUsuarioAndPeriodoIdPeriodoAndAsistencia(
            idDocente, idPeriodo, AsistenciaCita.ASISTIO)
        || procesoRepository.existeActaEnEstado(idDocente, idPeriodo, EstadoActa.FIRMADA);
  }

  private List<ProcesoEvaluacionResponse> armar(
      PeriodoAcademico periodo, List<DocenteResponse> docentes, boolean conCobertura) {
    if (docentes.isEmpty()) {
      return List.of();
    }
    Long idPeriodo = periodo.getIdPeriodo();
    List<Long> ids = docentes.stream().map(DocenteResponse::idDocente).toList();
    Map<Long, PromedioPonderadoResponse> promedios = promedioService.obtenerPromedios(idPeriodo);
    Map<Long, List<Cita>> citas =
        agrupar(procesoRepository.buscarCitas(idPeriodo, ids), Cita::getDocente);
    Map<Long, List<Acta>> actas =
        agrupar(procesoRepository.buscarActas(idPeriodo, ids), Acta::getDocente);
    Map<Long, CoberturaEstudiantes> coberturas =
        conCobertura
            ? procesoRepository.contarCobertura(idPeriodo, ids).stream()
                .collect(Collectors.toMap(CoberturaEstudiantes::getIdDocente, Function.identity()))
            : Map.of();
    Map<Long, OrigenCalificacion> origenes =
        conCobertura
            ? procesoRepository.buscarOrigenEstudiantes(idPeriodo, ids).stream()
                .collect(
                    Collectors.toMap(
                        OrigenNota::getIdDocente, o -> OrigenCalificacion.valueOf(o.getOrigen())))
            : Map.of();
    return docentes.stream()
        .map(
            docente -> {
              Long id = docente.idDocente();
              CoberturaEstudiantes cobertura = coberturas.get(id);
              return describir(
                  docente,
                  promedios.getOrDefault(id, PromedioPonderadoResponse.sinNota()),
                  citas.getOrDefault(id, List.of()),
                  actas.getOrDefault(id, List.of()),
                  cobertura == null
                      ? null
                      : new Cobertura(
                          cobertura.getRespondidas(),
                          cobertura.getEsperadas(),
                          heteroevaluacion.minimoRespuestas(),
                          origenes.get(id)));
            })
        .toList();
  }

  private static <T> Map<Long, List<T>> agrupar(
      Collection<T> elementos, Function<T, Usuario> docente) {
    return elementos.stream()
        .collect(Collectors.groupingBy(elemento -> docente.apply(elemento).getIdUsuario()));
  }

  private ProcesoEvaluacionResponse describir(
      DocenteResponse docente,
      PromedioPonderadoResponse promedio,
      List<Cita> citas,
      List<Acta> actas,
      Cobertura cobertura) {
    Optional<Cita> activa =
        citas.stream().filter(c -> c.getEstado() == EstadoCita.ACTIVA).findFirst();
    Optional<Cita> ultima = citas.stream().reduce((primera, segunda) -> segunda);
    Optional<Acta> acta =
        actas.stream().filter(a -> a.getEstado() != EstadoActa.ANULADA).findFirst();
    boolean asistio = citas.stream().anyMatch(Cita::asistio);
    boolean actaFirmada = acta.map(a -> a.getEstado() == EstadoActa.FIRMADA).orElse(false);

    EstadoProcesoEvaluacion estado =
        calcularEstado(promedio.fuentesPendientes(), activa, ultima, acta, asistio);
    boolean habilitado = !propiedades.exigirAsistencia() || asistio || actaFirmada;

    return new ProcesoEvaluacionResponse(
        docente,
        estado,
        estado.getNombre(),
        estado.getPaso(),
        promedio.fuentesPendientes(),
        cobertura,
        activa.or(() -> ultima).map(this::describirCita).orElse(null),
        acta.map(this::describirActa).orElse(null),
        habilitado,
        hitos(citas, actas));
  }

  private EstadoProcesoEvaluacion calcularEstado(
      List<FuenteEvaluacion> pendientes,
      Optional<Cita> activa,
      Optional<Cita> ultima,
      Optional<Acta> acta,
      boolean asistio) {
    if (acta.isPresent() && acta.get().getEstado() == EstadoActa.FIRMADA) {
      return EstadoProcesoEvaluacion.FINALIZADO;
    }
    if (acta.isPresent() && !acta.get().getFirmas().isEmpty()) {
      return EstadoProcesoEvaluacion.ACTA_EN_FIRMA;
    }
    if (asistio) {
      return EstadoProcesoEvaluacion.EN_REUNION;
    }
    if (pendientes.contains(FuenteEvaluacion.ESTUDIANTES)
        || pendientes.contains(FuenteEvaluacion.COORDINACION)) {
      return EstadoProcesoEvaluacion.EVALUACIONES_EN_CURSO;
    }
    if (pendientes.contains(FuenteEvaluacion.AUTOEVALUACION)) {
      return EstadoProcesoEvaluacion.PENDIENTE_AUTOEVALUACION;
    }
    if (activa.isPresent()) {
      return activa.get().getConfirmacion() == ConfirmacionCita.CONFIRMADA
          ? EstadoProcesoEvaluacion.ASISTENCIA_CONFIRMADA
          : EstadoProcesoEvaluacion.CITA_AGENDADA;
    }
    if (ultima.isPresent() && ultima.get().getMotivoLiberacion() == MotivoLiberacion.INASISTENCIA) {
      return EstadoProcesoEvaluacion.NO_ASISTIO;
    }
    return EstadoProcesoEvaluacion.EVALUACION_COMPLETA;
  }

  private CitaProceso describirCita(Cita cita) {
    return new CitaProceso(
        cita.getIdCita(),
        cita.getEstado() == EstadoCita.ACTIVA,
        cita.getFranja().getFecha(),
        cita.getFranja().getHoraInicio(),
        cita.getFranja().getHoraFin(),
        cita.getFranja().getCoordinador().getNombreCompleto(),
        cita.getConfirmacion(),
        cita.getFechaConfirmacion(),
        cita.getAsistencia(),
        cita.getFechaAsistencia());
  }

  private ActaProceso describirActa(Acta acta) {
    return new ActaProceso(
        acta.getIdActa(),
        acta.getTipo(),
        acta.getEstado(),
        acta.firmadaPor(Rol.DOCENTE),
        acta.firmadaPor(Rol.COORDINADOR),
        acta.getFechaFirma());
  }

  private List<Hito> hitos(List<Cita> citas, List<Acta> actas) {
    List<Hito> hitos = new ArrayList<>();
    for (Cita cita : citas) {
      String franja = describirFranja(cita);
      hitos.add(new Hito("RESERVA", "Escogió horario", franja, cita.getFechaReserva()));
      if (cita.getConfirmacion() == ConfirmacionCita.CONFIRMADA) {
        hitos.add(
            new Hito("CONFIRMACION", "Confirmó asistencia", franja, cita.getFechaConfirmacion()));
      }
      if (cita.getAsistencia() == AsistenciaCita.ASISTIO) {
        hitos.add(
            new Hito(
                "ASISTENCIA",
                "Asistió a la reunión",
                "La coordinación habilitó el resultado",
                cita.getFechaAsistencia()));
      }
      if (cita.getMotivoLiberacion() != null) {
        hitos.add(liberacion(cita, franja));
      }
    }
    for (Acta acta : actas) {
      String nombre =
          (acta.getTipo() == TipoActa.COMPROMISO
                  ? "Acta de compromiso"
                  : "Acta de retroalimentación")
              + " · versión "
              + acta.getVersion();
      hitos.add(new Hito("ACTA", "Acta generada", nombre, acta.getFechaCreacion()));
      for (FirmaActa firma : acta.getFirmas()) {
        hitos.add(
            new Hito(
                "FIRMA",
                firma.getRol() == Rol.DOCENTE ? "Firmó el docente" : "Firmó la coordinación",
                firma.getNombre(),
                firma.getFecha()));
      }
      if (acta.getEstado() != EstadoActa.BORRADOR && acta.getFechaFirma() != null) {
        hitos.add(
            new Hito(
                "CIERRE",
                "Acta firmada por las dos partes",
                "Código " + acta.getCodigoVerificacion(),
                acta.getFechaFirma()));
      }
    }
    hitos.removeIf(hito -> hito.fecha() == null);
    hitos.sort(Comparator.comparing(Hito::fecha));
    return hitos;
  }

  private Hito liberacion(Cita cita, String franja) {
    return switch (cita.getMotivoLiberacion()) {
      case REEMPLAZADA, LIBERADA_POR_DOCENTE ->
          new Hito("CAMBIO", "Cambió de horario", franja, cita.getFechaLiberacion());
      case DECLINADA_POR_DOCENTE ->
          new Hito(
              "DECLINACION", "Declinó la cita", franja + " quedó libre", cita.getFechaLiberacion());
      case INASISTENCIA ->
          new Hito("INASISTENCIA", "No asistió a la reunión", franja, cita.getFechaAsistencia());
    };
  }

  private String describirFranja(Cita cita) {
    return Objects.requireNonNull(cita.getFranja().getFecha()).format(FECHA)
        + " "
        + cita.getFranja().getHoraInicio().format(HORA)
        + "–"
        + cita.getFranja().getHoraFin().format(HORA);
  }
}
