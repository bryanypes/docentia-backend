package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.config.AnalisisComentariosProperties;
import com.uniautonoma.docentia.dto.AnalisisComentariosResponse;
import com.uniautonoma.docentia.dto.AnalisisComentariosResponse.Referencia;
import com.uniautonoma.docentia.dto.ComentariosEstudiantesResponse;
import com.uniautonoma.docentia.dto.ComentariosEstudiantesResponse.Comentario;
import com.uniautonoma.docentia.dto.ComentariosEstudiantesResponse.EtiquetaComentario;
import com.uniautonoma.docentia.dto.ResumenComentariosResponse;
import com.uniautonoma.docentia.dto.ResumenComentariosResponse.CriterioAgregado;
import com.uniautonoma.docentia.dto.ResumenComentariosResponse.TemaAgregado;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import com.uniautonoma.docentia.security.LimitadorSolicitudes;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.DocenteService;
import com.uniautonoma.docentia.service.VentanaEtapaService;
import com.uniautonoma.docentia.service.analisis.ResultadosAnalisisRepositorio.Guardado;
import com.uniautonoma.docentia.service.analisis.ResultadosAnalisisRepositorio.Pagina;
import com.uniautonoma.docentia.service.analisis.local.CatalogoDescriptores;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Advertencia;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Tema;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta del análisis de comentarios: el de un docente, sus comentarios anonimizados y el resumen
 * agregado. Quién entra lo decide {@link com.uniautonoma.docentia.security.PoliticaComentarios};
 * aquí se aplica el alcance (un coordinador solo ve su facultad; un docente ajeno responde 404
 * igual que uno inexistente).
 */
@Service
@Transactional(readOnly = true)
public class AnalisisComentariosService {

  private static final Set<String> SENTIMIENTOS = Set.of("POSITIVO", "NEGATIVO", "NEUTRO", "MIXTO");

  private final DocenteService docenteService;
  private final PeriodoAcademicoRepository periodoRepository;
  private final ProgramaRepository programaRepository;
  private final SolicitudAnalisisRepository solicitudRepository;
  private final ResultadosAnalisisRepositorio resultados;
  private final VentanaEtapaService ventanas;
  private final LimitadorSolicitudes limitador;
  private final AnalisisComentariosProperties propiedades;
  private final ComentarioEstudianteRepository comentarioRepository;

  public AnalisisComentariosService(
      DocenteService docenteService,
      PeriodoAcademicoRepository periodoRepository,
      ProgramaRepository programaRepository,
      SolicitudAnalisisRepository solicitudRepository,
      ResultadosAnalisisRepositorio resultados,
      VentanaEtapaService ventanas,
      LimitadorSolicitudes limitador,
      AnalisisComentariosProperties propiedades,
      ComentarioEstudianteRepository comentarioRepository) {
    this.comentarioRepository = comentarioRepository;
    this.docenteService = docenteService;
    this.periodoRepository = periodoRepository;
    this.programaRepository = programaRepository;
    this.solicitudRepository = solicitudRepository;
    this.resultados = resultados;
    this.ventanas = ventanas;
    this.limitador = limitador;
    this.propiedades = propiedades;
  }

  /**
   * Análisis vigente y estado de la última solicitud.
   *
   * @throws RecursoNoEncontradoException si el docente está fuera del alcance o el periodo no
   *     existe
   */
  public AnalisisComentariosResponse analisis(
      Long idDocente, Long idPeriodo, UsuarioSesion sesion) {
    limitar(sesion);
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    PeriodoAcademico periodo = periodo(idPeriodo);
    Optional<SolicitudAnalisis> ultima = ultima(idDocente, idPeriodo);
    Estado estado = estado(ultima, periodo, idDocente);
    Optional<Guardado> vigente = resultados.vigente(idDocente, idPeriodo);
    String estadoHallazgos =
        vigente
            .flatMap(g -> solicitudRepository.findById(g.idSolicitud()))
            .map(SolicitudAnalisis::getEstadoBorrador)
            .orElse(null);
    Programa programa = docente.getPrograma();
    return new AnalisisComentariosResponse(
        estado.codigo(),
        estado.motivo(),
        estado.mensaje(),
        idDocente,
        docente.getNombreCompleto(),
        idPeriodo,
        periodo.getNombre(),
        new Referencia(programa.getIdPrograma(), programa.getNombre()),
        new Referencia(programa.getFacultad().getIdFacultad(), programa.getFacultad().getNombre()),
        vigente.map(Guardado::fuente).orElse(ultima.map(SolicitudAnalisis::getFuente).orElse(null)),
        ultima.map(SolicitudAnalisis::getIdSolicitud).orElse(null),
        vigente.map(Guardado::version).orElse(null),
        vigente.map(Guardado::generadoEn).orElse(null),
        estadoHallazgos,
        vigente.map(Guardado::resultado).orElse(null));
  }

  /**
   * Página de comentarios anonimizados del análisis vigente.
   *
   * @throws DatosInvalidosException si el criterio, el sentimiento o la página no son válidos
   */
  public ComentariosEstudiantesResponse comentarios(
      Long idDocente,
      Long idPeriodo,
      String criterio,
      String sentimiento,
      int pagina,
      int tamano,
      UsuarioSesion sesion) {
    limitar(sesion);
    if (criterio != null && !CatalogoDescriptores.CRITERIOS.containsKey(criterio)) {
      throw new DatosInvalidosException("Criterio desconocido", "criterio");
    }
    if (sentimiento != null && !SENTIMIENTOS.contains(sentimiento)) {
      throw new DatosInvalidosException(
          "El sentimiento debe ser POSITIVO, NEGATIVO, NEUTRO o MIXTO", "sentimiento");
    }
    if (pagina < 0 || tamano < 1) {
      throw new DatosInvalidosException("La página empieza en 0 y el tamaño en 1", "page");
    }
    int tamanoEfectivo = Math.min(tamano, propiedades.tamanoMaximoPagina());
    docenteService.buscarDocenteVisible(idDocente, sesion);
    PeriodoAcademico periodo = periodo(idPeriodo);
    Optional<Guardado> vigente = resultados.vigente(idDocente, idPeriodo);
    if (vigente.isEmpty()) {
      Estado estado = estado(ultima(idDocente, idPeriodo), periodo, idDocente);
      return new ComentariosEstudiantesResponse(
          false,
          estado.codigo(),
          idDocente,
          idPeriodo,
          null,
          null,
          pagina,
          tamanoEfectivo,
          0,
          List.of());
    }
    Pagina filas =
        resultados.comentarios(vigente.get().id(), criterio, sentimiento, pagina, tamanoEfectivo);
    List<Comentario> comentarios =
        filas.comentarios().stream()
            .map(
                c ->
                    new Comentario(
                        c.indice(),
                        c.texto(),
                        c.sentimiento(),
                        c.etiquetas().stream()
                            .map(
                                e ->
                                    new EtiquetaComentario(
                                        e.criterio(),
                                        e.descriptor(),
                                        nombreDescriptor(e.descriptor()),
                                        e.polaridad()))
                            .toList()))
            .toList();
    return new ComentariosEstudiantesResponse(
        true,
        null,
        idDocente,
        idPeriodo,
        vigente.get().fuente(),
        vigente.get().version(),
        pagina,
        tamanoEfectivo,
        filas.total(),
        comentarios);
  }

  /**
   * Resumen agregado del periodo. Un coordinador queda en su facultad aunque pida otro programa.
   *
   * @throws RecursoNoEncontradoException si el periodo no existe o el programa está fuera del
   *     alcance
   */
  public ResumenComentariosResponse resumen(Long idPeriodo, Long idPrograma, UsuarioSesion sesion) {
    limitar(sesion);
    periodo(idPeriodo);
    Referencia programa = null;
    if (idPrograma != null) {
      Programa encontrado =
          programaRepository
              .findById(idPrograma)
              .filter(p -> sesion.tieneAlcanceSobreFacultad(p.getFacultad().getIdFacultad()))
              .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado"));
      programa = new Referencia(encontrado.getIdPrograma(), encontrado.getNombre());
    }
    Long idFacultad = sesion.esCoordinador() ? sesion.idFacultad() : null;
    List<ResultadoAnalisis> vigentes =
        resultados.vigentesDelPeriodo(idPeriodo, idPrograma, idFacultad);
    int minimo = propiedades.minimoDocentesResumen();
    if (vigentes.size() < minimo) {
      return new ResumenComentariosResponse(
          false,
          "GRUPO_PEQUENO",
          idPeriodo,
          programa,
          vigentes.size(),
          minimo,
          0,
          Map.of(),
          null,
          List.of(),
          List.of(),
          Map.of());
    }
    return agregar(idPeriodo, programa, vigentes, minimo);
  }

  private ResumenComentariosResponse agregar(
      Long idPeriodo, Referencia programa, List<ResultadoAnalisis> vigentes, int minimo) {
    int comentarios = 0;
    int positivos = 0;
    int negativos = 0;
    int neutros = 0;
    int mixtos = 0;
    Map<String, Integer> excluidos = new TreeMap<>();
    Map<String, Integer> advertencias = new TreeMap<>();
    Map<String, int[]> temas = new LinkedHashMap<>();
    Map<String, Tema> ejemplo = new LinkedHashMap<>();
    Map<String, int[]> criterios = new TreeMap<>();
    for (ResultadoAnalisis r : vigentes) {
      comentarios += r.nComentarios();
      positivos += r.sentimientoGlobal().positivos();
      negativos += r.sentimientoGlobal().negativos();
      neutros += r.sentimientoGlobal().neutros();
      mixtos += r.sentimientoGlobal().mixtos();
      r.nExcluidos().forEach((motivo, n) -> excluidos.merge(motivo, n, Integer::sum));
      for (Advertencia a : r.advertencias()) {
        advertencias.merge(a.tipo(), a.cantidad(), Integer::sum);
      }
      for (Tema t : r.temas()) {
        String clave = t.criterio() + "|" + t.descriptor() + "|" + t.polaridad();
        int[] suma = temas.computeIfAbsent(clave, k -> new int[2]);
        suma[0] += t.frecuencia();
        suma[1]++;
        ejemplo.putIfAbsent(clave, t);
      }
      for (ResultadoAnalisis.Criterio c : r.criterios()) {
        int[] suma = criterios.computeIfAbsent(c.codigo(), k -> new int[3]);
        suma[0] += c.positivos();
        suma[1] += c.negativos();
        suma[2] += c.neutros();
      }
    }
    int total = comentarios;
    List<TemaAgregado> agregados = new ArrayList<>();
    temas.forEach(
        (clave, suma) -> {
          Tema t = ejemplo.get(clave);
          agregados.add(
              new TemaAgregado(
                  t.criterio(),
                  t.descriptor(),
                  t.nombre(),
                  t.polaridad(),
                  suma[0],
                  total == 0 ? 0 : Math.round(suma[0] * 1000.0 / total) / 10.0,
                  suma[1]));
        });
    agregados.sort(
        Comparator.comparingInt(TemaAgregado::frecuencia)
            .reversed()
            .thenComparing(TemaAgregado::descriptor)
            .thenComparing(TemaAgregado::polaridad));
    List<CriterioAgregado> porCriterio =
        criterios.entrySet().stream()
            .map(
                e ->
                    new CriterioAgregado(
                        e.getKey(),
                        CatalogoDescriptores.CRITERIOS.get(e.getKey()),
                        e.getValue()[0],
                        e.getValue()[1],
                        e.getValue()[2]))
            .toList();
    return new ResumenComentariosResponse(
        true,
        null,
        idPeriodo,
        programa,
        vigentes.size(),
        minimo,
        comentarios,
        excluidos,
        new ResumenComentariosResponse.Sentimiento(positivos, negativos, neutros, mixtos),
        agregados,
        porCriterio,
        advertencias);
  }

  private record Estado(String codigo, String motivo, String mensaje) {}

  /**
   * Sin solicitud, la recolección abierta solo frena a quien no tiene comentarios cargados: esos ya
   * se pueden analizar; los del formulario esperan el cierre.
   */
  private Estado estado(
      Optional<SolicitudAnalisis> ultima, PeriodoAcademico periodo, Long idDocente) {
    if (ultima.isEmpty()) {
      boolean hayCargados =
          comentarioRepository.countByDocenteIdUsuarioAndPeriodoIdPeriodo(
                  idDocente, periodo.getIdPeriodo())
              > 0;
      return hayCargados || ventanas.haTerminado(periodo, TipoEtapa.RECOLECCION_CALIFICACIONES)
          ? new Estado(
              "NO_DISPONIBLE_AUN", "SIN_SOLICITAR", "Todavía no se ha solicitado el análisis.")
          : new Estado(
              "NO_DISPONIBLE_AUN",
              "VENTANA_ABIERTA",
              "El análisis estará disponible cuando cierre la recolección de evaluaciones.");
    }
    SolicitudAnalisis solicitud = ultima.get();
    String motivo = solicitud.getMotivo();
    return switch (solicitud.getEstado()) {
      case EN_COLA -> new Estado("EN_COLA", null, "El análisis está en cola.");
      case EN_PROCESO -> new Estado("EN_PROCESO", null, "El análisis se está generando.");
      case COMPLETADO -> new Estado("COMPLETADO", null, null);
      case ERROR -> new Estado("ERROR", null, solicitud.getMensaje());
      case INSUFICIENTE -> {
        if ("SIN_COMENTARIOS".equals(motivo) || "TODOS_EXCLUIDOS".equals(motivo)) {
          yield new Estado("SIN_COMENTARIOS", motivo, solicitud.getMensaje());
        }
        if ("VENTANA_ABIERTA".equals(motivo)) {
          yield new Estado("NO_DISPONIBLE_AUN", motivo, solicitud.getMensaje());
        }
        yield new Estado(
            "INSUFICIENTE", motivo == null ? "POCAS_ENCUESTAS" : motivo, solicitud.getMensaje());
      }
    };
  }

  private Optional<SolicitudAnalisis> ultima(Long idDocente, Long idPeriodo) {
    return solicitudRepository
        .findByDocenteIdUsuarioAndPeriodoIdPeriodoOrderByIdSolicitudDesc(idDocente, idPeriodo)
        .stream()
        .findFirst();
  }

  private PeriodoAcademico periodo(Long idPeriodo) {
    return periodoRepository
        .findById(idPeriodo)
        .orElseThrow(() -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
  }

  private void limitar(UsuarioSesion sesion) {
    limitador.registrar(
        "analisis-comentarios:" + sesion.idUsuario(), propiedades.consultasPorMinuto());
  }

  private static String nombreDescriptor(String codigo) {
    CatalogoDescriptores.Descriptor descriptor = CatalogoDescriptores.DESCRIPTORES.get(codigo);
    return descriptor == null ? null : descriptor.nombre();
  }
}
