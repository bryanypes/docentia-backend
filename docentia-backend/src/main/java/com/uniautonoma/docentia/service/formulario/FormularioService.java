package com.uniautonoma.docentia.service.formulario;

import com.uniautonoma.docentia.dto.DocenteResponse;
import com.uniautonoma.docentia.dto.FormularioRequest;
import com.uniautonoma.docentia.dto.FormularioResponse;
import com.uniautonoma.docentia.dto.FormularioResponse.CriterioFormulario;
import com.uniautonoma.docentia.dto.FormularioResponse.DescriptorFormulario;
import com.uniautonoma.docentia.dto.FormularioResponse.OpcionEscala;
import com.uniautonoma.docentia.dto.PendientesFormularioResponse;
import com.uniautonoma.docentia.dto.PendientesFormularioResponse.DocenteAvance;
import com.uniautonoma.docentia.dto.PendientesFormularioResponse.ProgramaAvance;
import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.entity.EstadoFormulario;
import com.uniautonoma.docentia.entity.EvaluacionFormulario;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.NoProcesableException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.EvaluacionFormularioRepository;
import com.uniautonoma.docentia.repository.EvaluacionFormularioRepository.AvanceFormulario;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.CalificacionService;
import com.uniautonoma.docentia.service.DocenteService;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import com.uniautonoma.docentia.service.VentanaEtapaService;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autoevaluación y coevaluación del Anexo 1. Se editan solo en el periodo activo y con la
 * recolección abierta. Al enviarse, la media de los descriptores se vuelve la nota de la fuente.
 */
@Service
@Transactional
public class FormularioService {

  private static final int DECIMALES_FUENTE = 4;
  private static final int MAXIMO_COMENTARIO = 1000;
  private static final String NO_ENCONTRADO = "Formulario no encontrado";

  private final EvaluacionFormularioRepository formularioRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final UsuarioRepository usuarioRepository;
  private final PeriodoAcademicoService periodoService;
  private final DocenteService docenteService;
  private final VentanaEtapaService ventanaEtapaService;
  private final CalificacionService calificacionService;
  private final CatalogoInstrumento catalogo;
  private final ApplicationEventPublisher eventos;
  private final Clock reloj;
  private final List<String> etiquetasEscala;

  public FormularioService(
      EvaluacionFormularioRepository formularioRepository,
      PeriodoAcademicoRepository periodoRepository,
      UsuarioRepository usuarioRepository,
      PeriodoAcademicoService periodoService,
      DocenteService docenteService,
      VentanaEtapaService ventanaEtapaService,
      CalificacionService calificacionService,
      CatalogoInstrumento catalogo,
      ApplicationEventPublisher eventos,
      Clock reloj,
      @Value("${docentia.formularios.etiquetas-escala}") List<String> etiquetasEscala) {
    this.formularioRepository = formularioRepository;
    this.periodoRepository = periodoRepository;
    this.usuarioRepository = usuarioRepository;
    this.periodoService = periodoService;
    this.docenteService = docenteService;
    this.ventanaEtapaService = ventanaEtapaService;
    this.calificacionService = calificacionService;
    this.catalogo = catalogo;
    this.eventos = eventos;
    this.reloj = reloj;
    this.etiquetasEscala = etiquetasEscala;
  }

  @Transactional(readOnly = true)
  public FormularioResponse leerAutoevaluacion(Long idPeriodo, UsuarioSesion sesion) {
    PeriodoAcademico periodo = resolverPeriodo(idPeriodo);
    Usuario docente = docentePropio(sesion);
    return responder(
        periodo, docente, TipoFormulario.AUTO, buscar(periodo, docente, TipoFormulario.AUTO));
  }

  public FormularioResponse guardarAutoevaluacion(
      Long idPeriodo, FormularioRequest solicitud, UsuarioSesion sesion) {
    return guardar(
        resolverPeriodo(idPeriodo), docentePropio(sesion), TipoFormulario.AUTO, solicitud, sesion);
  }

  public FormularioResponse enviarAutoevaluacion(Long idPeriodo, UsuarioSesion sesion) {
    return enviar(resolverPeriodo(idPeriodo), docentePropio(sesion), TipoFormulario.AUTO, sesion);
  }

  @Transactional(readOnly = true)
  public FormularioResponse leerCoevaluacion(Long idPeriodo, Long idDocente, UsuarioSesion sesion) {
    PeriodoAcademico periodo = resolverPeriodo(idPeriodo);
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    return responder(
        periodo, docente, TipoFormulario.COEVAL, buscarCoevaluacion(periodo, docente, sesion));
  }

  public FormularioResponse guardarCoevaluacion(
      Long idPeriodo, Long idDocente, FormularioRequest solicitud, UsuarioSesion sesion) {
    PeriodoAcademico periodo = resolverPeriodo(idPeriodo);
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    buscarCoevaluacion(periodo, docente, sesion);
    return guardar(periodo, docente, TipoFormulario.COEVAL, solicitud, sesion);
  }

  public FormularioResponse enviarCoevaluacion(
      Long idPeriodo, Long idDocente, UsuarioSesion sesion) {
    PeriodoAcademico periodo = resolverPeriodo(idPeriodo);
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    buscarCoevaluacion(periodo, docente, sesion);
    return enviar(periodo, docente, TipoFormulario.COEVAL, sesion);
  }

  /**
   * Avance sin puntajes. El coordinador ve a los docentes de su facultad; el vicerrector, solo
   * conteos por programa.
   */
  @Transactional(readOnly = true)
  public PendientesFormularioResponse pendientes(Long idPeriodo, UsuarioSesion sesion) {
    PeriodoAcademico periodo = resolverPeriodo(idPeriodo);
    List<DocenteResponse> docentes = docenteService.listarDocentes(null, sesion);
    List<AvanceFormulario> avance = formularioRepository.leerAvance(periodo.getIdPeriodo());
    Map<Long, AvanceFormulario> coevaluaciones = delTipo(avance, TipoFormulario.COEVAL);
    Map<Long, AvanceFormulario> autoevaluaciones = delTipo(avance, TipoFormulario.AUTO);

    List<DocenteAvance> filas =
        docentes.stream()
            .map(
                docente -> {
                  AvanceFormulario fila = coevaluaciones.get(docente.idDocente());
                  return new DocenteAvance(
                      docente.idDocente(),
                      docente.nombreCompleto(),
                      docente.nombrePrograma(),
                      fila == null ? "SIN_INICIAR" : fila.estado().name(),
                      fila == null ? 0 : fila.respondidos());
                })
            .toList();
    Map<String, List<DocenteResponse>> porPrograma =
        docentes.stream()
            .collect(
                Collectors.groupingBy(
                    DocenteResponse::nombrePrograma, LinkedHashMap::new, Collectors.toList()));
    List<ProgramaAvance> programas =
        porPrograma.entrySet().stream()
            .map(
                programa ->
                    resumir(
                        programa.getKey(), programa.getValue(), coevaluaciones, autoevaluaciones))
            .toList();
    long pendientes = filas.stream().filter(fila -> !"ENVIADA".equals(fila.estado())).count();
    return new PendientesFormularioResponse(
        PeriodoResumenResponse.desde(periodo),
        total(catalogo.de(TipoFormulario.COEVAL)),
        pendientes,
        sesion.esCoordinador() ? filas : List.of(),
        programas);
  }

  private static Map<Long, AvanceFormulario> delTipo(
      List<AvanceFormulario> avance, TipoFormulario tipo) {
    return avance.stream()
        .filter(fila -> fila.tipo() == tipo)
        .collect(Collectors.toMap(AvanceFormulario::idDocente, Function.identity()));
  }

  private static ProgramaAvance resumir(
      String programa,
      List<DocenteResponse> docentes,
      Map<Long, AvanceFormulario> coevaluaciones,
      Map<Long, AvanceFormulario> autoevaluaciones) {
    long borrador = 0;
    long enviadas = 0;
    long autoevaluacionesEnviadas = 0;
    for (DocenteResponse docente : docentes) {
      AvanceFormulario coevaluacion = coevaluaciones.get(docente.idDocente());
      if (coevaluacion != null) {
        if (coevaluacion.estado() == EstadoFormulario.ENVIADA) {
          enviadas++;
        } else {
          borrador++;
        }
      }
      AvanceFormulario autoevaluacion = autoevaluaciones.get(docente.idDocente());
      if (autoevaluacion != null && autoevaluacion.estado() == EstadoFormulario.ENVIADA) {
        autoevaluacionesEnviadas++;
      }
    }
    return new ProgramaAvance(
        programa,
        docentes.get(0).nombreFacultad(),
        docentes.size(),
        docentes.size() - borrador - enviadas,
        borrador,
        enviadas,
        autoevaluacionesEnviadas);
  }

  private PeriodoAcademico resolverPeriodo(Long idPeriodo) {
    return idPeriodo == null
        ? periodoService.exigirPeriodoActivo()
        : periodoRepository
            .findById(idPeriodo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
  }

  private Usuario docentePropio(UsuarioSesion sesion) {
    return usuarioRepository.findById(sesion.idUsuario()).orElseThrow();
  }

  private Optional<EvaluacionFormulario> buscar(
      PeriodoAcademico periodo, Usuario docente, TipoFormulario tipo) {
    return formularioRepository.findByPeriodoIdPeriodoAndDocenteIdUsuarioAndTipo(
        periodo.getIdPeriodo(), docente.getIdUsuario(), tipo);
  }

  /** Cada coordinador solo alcanza la coevaluación que él mismo empezó. */
  private Optional<EvaluacionFormulario> buscarCoevaluacion(
      PeriodoAcademico periodo, Usuario docente, UsuarioSesion sesion) {
    Optional<EvaluacionFormulario> encontrada = buscar(periodo, docente, TipoFormulario.COEVAL);
    if (encontrada.isPresent()
        && !encontrada.get().getEvaluador().getIdUsuario().equals(sesion.idUsuario())) {
      throw new RecursoNoEncontradoException(NO_ENCONTRADO);
    }
    return encontrada;
  }

  private void exigirEditable(PeriodoAcademico periodo) {
    if (!periodo.getEsActivo()) {
      throw new ReglaNegocioException("Los formularios solo se responden en el periodo activo");
    }
    ventanaEtapaService.exigirEtapaAbierta(periodo, TipoEtapa.RECOLECCION_CALIFICACIONES);
  }

  private FormularioResponse guardar(
      PeriodoAcademico periodo,
      Usuario docente,
      TipoFormulario tipo,
      FormularioRequest solicitud,
      UsuarioSesion sesion) {
    exigirEditable(periodo);
    List<CatalogoInstrumento.Criterio> criterios = catalogo.de(tipo);
    Map<Long, Short> respuestas = validarRespuestas(solicitud.respuestas(), criterios);
    Map<Long, String> comentarios = validarComentarios(solicitud.comentarios(), criterios);
    EvaluacionFormulario formulario =
        buscar(periodo, docente, tipo)
            .orElseGet(
                () ->
                    new EvaluacionFormulario(
                        periodo,
                        docente,
                        usuarioRepository.getReferenceById(sesion.idUsuario()),
                        tipo,
                        CatalogoInstrumento.VERSION));
    exigirBorrador(formulario);
    formulario.reemplazar(respuestas, comentarios);
    EvaluacionFormulario guardado = formularioRepository.saveAndFlush(formulario);
    return responder(periodo, docente, tipo, Optional.of(guardado));
  }

  private FormularioResponse enviar(
      PeriodoAcademico periodo, Usuario docente, TipoFormulario tipo, UsuarioSesion sesion) {
    exigirEditable(periodo);
    int total = total(catalogo.de(tipo));
    EvaluacionFormulario formulario =
        buscar(periodo, docente, tipo)
            .orElseThrow(
                () ->
                    new NoProcesableException("Responde los " + total + " ítems antes de enviar"));
    exigirBorrador(formulario);
    int respondidos = formulario.getRespuestas().size();
    if (respondidos < total) {
      throw new NoProcesableException(
          "Faltan " + (total - respondidos) + " de " + total + " ítems por responder");
    }
    BigDecimal media = media(formulario.getRespuestas());
    formulario.enviar(media, LocalDateTime.now(reloj));
    EvaluacionFormulario enviado = formularioRepository.saveAndFlush(formulario);
    calificacionService.registrarDesdeFormulario(
        docente, periodo, tipo.getFuente(), media, formulario.getEvaluador());
    if (tipo == TipoFormulario.AUTO) {
      eventos.publishEvent(
          EventoNotificacion.aCoordinadores(
              TipoNotificacion.AUTOEVALUACION_ENVIADA,
              docente,
              "/coevaluaciones",
              "FORMULARIO",
              enviado.getId(),
              "formulario:" + enviado.getId()));
    }
    return responder(periodo, docente, tipo, Optional.of(enviado));
  }

  private static void exigirBorrador(EvaluacionFormulario formulario) {
    if (formulario.estaEnviada()) {
      throw new ReglaNegocioException("El formulario ya se envió y no cambia");
    }
  }

  private static BigDecimal media(Map<Long, Short> respuestas) {
    int suma = respuestas.values().stream().mapToInt(Short::intValue).sum();
    return BigDecimal.valueOf(suma)
        .divide(BigDecimal.valueOf(respuestas.size()), DECIMALES_FUENTE, RoundingMode.HALF_UP);
  }

  private static int total(List<CatalogoInstrumento.Criterio> criterios) {
    return criterios.stream().mapToInt(criterio -> criterio.descriptores().size()).sum();
  }

  private static Map<Long, Short> validarRespuestas(
      Map<String, BigDecimal> respuestas, List<CatalogoInstrumento.Criterio> criterios) {
    Map<String, Long> descriptores = new HashMap<>();
    criterios.forEach(
        criterio ->
            criterio
                .descriptores()
                .forEach(descriptor -> descriptores.put(descriptor.codigo(), descriptor.id())));
    Map<Long, Short> validas = new HashMap<>();
    if (respuestas == null) {
      return validas;
    }
    respuestas.forEach(
        (codigo, valor) -> {
          Long id = descriptores.get(codigo);
          if (id == null) {
            throw new NoProcesableException(
                "El ítem " + codigo + " no pertenece a este formulario");
          }
          if (valor == null
              || valor.stripTrailingZeros().scale() > 0
              || valor.compareTo(BigDecimal.ONE) < 0
              || valor.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new NoProcesableException("Cada ítem se califica con un entero de 1 a 5");
          }
          validas.put(id, valor.shortValue());
        });
    return validas;
  }

  private static Map<Long, String> validarComentarios(
      Map<String, String> comentarios, List<CatalogoInstrumento.Criterio> criterios) {
    Map<Long, String> validos = new HashMap<>();
    if (comentarios == null) {
      return validos;
    }
    Map<String, Long> ids =
        criterios.stream()
            .collect(
                Collectors.toMap(
                    CatalogoInstrumento.Criterio::codigo, CatalogoInstrumento.Criterio::id));
    comentarios.forEach(
        (codigo, texto) -> {
          Long id = ids.get(codigo);
          if (id == null) {
            throw new NoProcesableException(
                "El criterio " + codigo + " no pertenece a este formulario");
          }
          if (texto != null && texto.length() > MAXIMO_COMENTARIO) {
            throw new NoProcesableException(
                "El comentario no puede superar los " + MAXIMO_COMENTARIO + " caracteres");
          }
          if (texto != null && !texto.isBlank()) {
            validos.put(id, texto.trim());
          }
        });
    return validos;
  }

  private FormularioResponse responder(
      PeriodoAcademico periodo,
      Usuario docente,
      TipoFormulario tipo,
      Optional<EvaluacionFormulario> formulario) {
    List<CatalogoInstrumento.Criterio> criterios = catalogo.de(tipo);
    Map<Long, Short> respuestas =
        formulario.map(EvaluacionFormulario::getRespuestas).orElse(Map.of());
    Map<Long, String> comentarios =
        formulario.map(EvaluacionFormulario::getComentarios).orElse(Map.of());
    List<CriterioFormulario> lista = new ArrayList<>();
    for (CatalogoInstrumento.Criterio criterio : criterios) {
      lista.add(
          new CriterioFormulario(
              criterio.codigo(),
              criterio.nombre(),
              comentarios.get(criterio.id()),
              criterio.descriptores().stream()
                  .map(
                      descriptor -> {
                        Short valor = respuestas.get(descriptor.id());
                        return new DescriptorFormulario(
                            descriptor.codigo(),
                            descriptor.texto(),
                            valor == null ? null : valor.intValue());
                      })
                  .toList()));
    }
    return new FormularioResponse(
        PeriodoResumenResponse.desde(periodo),
        tipo,
        docente.getIdUsuario(),
        docente.getNombreCompleto(),
        formulario.map(EvaluacionFormulario::getEstado).orElse(null),
        CatalogoInstrumento.VERSION,
        formulario.map(EvaluacionFormulario::getEnviadaEn).orElse(null),
        recoleccionAbierta(periodo),
        respuestas.size(),
        total(criterios),
        IntStream.range(0, etiquetasEscala.size())
            .mapToObj(i -> new OpcionEscala(i + 1, etiquetasEscala.get(i)))
            .toList(),
        lista);
  }

  private boolean recoleccionAbierta(PeriodoAcademico periodo) {
    return periodo.getEsActivo()
        && ventanaEtapaService.describirVentanas(periodo).stream()
            .anyMatch(
                ventana ->
                    ventana.etapa() == TipoEtapa.RECOLECCION_CALIFICACIONES && ventana.abierta());
  }
}
