package com.uniautonoma.docentia.service.importacion;

import com.uniautonoma.docentia.config.ImportacionProperties;
import com.uniautonoma.docentia.dto.DocenteImportacionResponse;
import com.uniautonoma.docentia.dto.ErrorFilaResponse;
import com.uniautonoma.docentia.dto.ImportacionResponse;
import com.uniautonoma.docentia.dto.ReporteImportacionResponse;
import com.uniautonoma.docentia.dto.ResultadoImportacionResponse;
import com.uniautonoma.docentia.entity.Importacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.ResultadoImportacion;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.ArchivoDemasiadoGrandeException;
import com.uniautonoma.docentia.exception.EtapaCerradaException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
import com.uniautonoma.docentia.repository.ImportacionRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.CalificacionService;
import com.uniautonoma.docentia.service.CalificacionService.ResultadoRegistro;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import com.uniautonoma.docentia.service.VentanaEtapaService;
import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes.DatosDocente;
import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes.ResultadoValidacion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Carga del CSV de evaluación de estudiantes exportado de Power BI. Primero se valida todo el
 * archivo; si hay un solo error no se guarda nada. Cada intento de importar queda registrado.
 */
@Service
@Transactional
public class ImportacionService {

  static final int ERRORES_MAXIMOS_EN_REPORTE = 500;

  private final LectorCsv lectorCsv;
  private final ValidadorEvaluacionEstudiantes validador;
  private final ImportacionProperties propiedades;
  private final UsuarioRepository usuarioRepository;
  private final ImportacionRepository importacionRepository;
  private final ComentarioEstudianteRepository comentarioRepository;
  private final CalificacionService calificacionService;
  private final PeriodoAcademicoService periodoService;
  private final VentanaEtapaService ventanaEtapaService;
  private final PlantillaMapeoService plantillaService;
  private final ResolutorMapeo resolutorMapeo = new ResolutorMapeo();

  public ImportacionService(
      PlantillaMapeoService plantillaService,
      LectorCsv lectorCsv,
      ValidadorEvaluacionEstudiantes validador,
      ImportacionProperties propiedades,
      UsuarioRepository usuarioRepository,
      ImportacionRepository importacionRepository,
      ComentarioEstudianteRepository comentarioRepository,
      CalificacionService calificacionService,
      PeriodoAcademicoService periodoService,
      VentanaEtapaService ventanaEtapaService) {
    this.plantillaService = plantillaService;
    this.lectorCsv = lectorCsv;
    this.validador = validador;
    this.propiedades = propiedades;
    this.usuarioRepository = usuarioRepository;
    this.importacionRepository = importacionRepository;
    this.comentarioRepository = comentarioRepository;
    this.calificacionService = calificacionService;
    this.periodoService = periodoService;
    this.ventanaEtapaService = ventanaEtapaService;
  }

  /**
   * Revisa el archivo sin guardar nada.
   *
   * @param archivo CSV subido
   * @param mapeoJson mapeo columna → campo como JSON, o nulo
   * @param nombrePlantilla plantilla propia a aplicar si no se envía mapeo, o nulo
   * @param sesion coordinador; solo sus docentes cuentan como encontrados
   * @return columnas detectadas, mapeo, errores por fila y vista previa por docente
   * @throws ArchivoDemasiadoGrandeException si supera el tamaño permitido
   */
  @Transactional(readOnly = true)
  public ReporteImportacionResponse validar(
      MultipartFile archivo, String mapeoJson, String nombrePlantilla, UsuarioSesion sesion) {
    Analisis analisis = analizar(archivo, mapeoJson, nombrePlantilla, sesion);
    Optional<PeriodoAcademico> periodo = periodoService.buscarPeriodoActivo();
    if (periodo.isEmpty()) {
      analisis.advertencias.add(
          "No hay un periodo académico activo: el archivo no se podrá importar");
    } else {
      avisarSiYaSeImporto(analisis, periodo.get());
      avisarSiLaEtapaEstaCerrada(analisis, periodo.get());
    }
    return analisis.reporte();
  }

  /**
   * Importa el archivo en el periodo activo. Todo o nada: con un error no se guarda ninguna nota ni
   * comentario, pero la carga queda registrada como rechazada.
   *
   * @param archivo CSV subido
   * @param mapeoJson mapeo columna → campo como JSON, o nulo
   * @param nombrePlantilla plantilla propia a aplicar si no se envía mapeo, o nulo
   * @param sesion coordinador que carga
   * @return el registro de la carga y su reporte; {@link ResultadoImportacionResponse#fueExitosa()}
   *     dice si se guardó
   * @throws ReglaNegocioException si no hay periodo activo
   * @throws EtapaCerradaException si la recolección de calificaciones no está abierta
   * @throws ArchivoDemasiadoGrandeException si supera el tamaño permitido
   */
  public ResultadoImportacionResponse importar(
      MultipartFile archivo, String mapeoJson, String nombrePlantilla, UsuarioSesion sesion) {
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    ventanaEtapaService.exigirEtapaAbierta(periodo, TipoEtapa.RECOLECCION_CALIFICACIONES);
    Analisis analisis = analizar(archivo, mapeoJson, nombrePlantilla, sesion);
    avisarSiYaSeImporto(analisis, periodo);
    ReporteImportacionResponse reporte = analisis.reporte();

    Importacion importacion =
        importacionRepository.saveAndFlush(
            new Importacion(
                analisis.nombreArchivo,
                analisis.hash,
                periodo,
                usuarioRepository.getReferenceById(sesion.idUsuario()),
                reporte.valido() ? ResultadoImportacion.EXITOSA : ResultadoImportacion.RECHAZADA,
                analisis.codificacion,
                analisis.delimitador,
                reporte.filasProcesadas(),
                reporte.filasConError()));
    if (reporte.valido()) {
      guardarDatos(analisis.validacion.docentes(), periodo, importacion, sesion);
    }
    return new ResultadoImportacionResponse(ImportacionResponse.desde(importacion), reporte);
  }

  /**
   * Lista las cargas. El coordinador ve las suyas; la vicerrectoría, todas.
   *
   * @param sesion usuario que consulta
   * @param idPeriodo filtro opcional por periodo
   * @return cargas de la más reciente a la más antigua
   */
  @Transactional(readOnly = true)
  public List<ImportacionResponse> listarImportaciones(UsuarioSesion sesion, Long idPeriodo) {
    Long idUsuario = sesion.esVicerrector() ? null : sesion.idUsuario();
    return importacionRepository.buscar(idUsuario, idPeriodo).stream()
        .map(ImportacionResponse::desde)
        .toList();
  }

  private void guardarDatos(
      List<DatosDocente> docentes,
      PeriodoAcademico periodo,
      Importacion importacion,
      UsuarioSesion sesion) {
    String motivo =
        "Carga de CSV #"
            + importacion.getIdImportacion()
            + " ("
            + importacion.getNombreArchivo()
            + ")";
    Map<ResultadoRegistro, Integer> notas = new EnumMap<>(ResultadoRegistro.class);
    int comentariosNuevos = 0;
    int comentariosRepetidos = 0;
    int docentesAfectados = 0;
    for (DatosDocente datos : docentes) {
      boolean cambio = false;
      if (datos.notas() != null) {
        ResultadoRegistro resultado =
            calificacionService.registrarCalificacionEstudiantes(
                datos.docente(), periodo, datos.notas(), sesion.idUsuario(), motivo, importacion);
        notas.merge(resultado, 1, Integer::sum);
        cambio = resultado != ResultadoRegistro.SIN_CAMBIOS;
      }
      Map<String, Integer> apariciones = new HashMap<>();
      for (String comentario : datos.comentarios()) {
        int aparicion =
            apariciones.merge(
                ValidadorEvaluacionEstudiantes.normalizarComentario(comentario), 1, Integer::sum);
        int insertados =
            comentarioRepository.insertarSiNoExiste(
                datos.docente().getIdUsuario(),
                periodo.getIdPeriodo(),
                comentario,
                ValidadorEvaluacionEstudiantes.huellaComentario(comentario, aparicion),
                importacion.getIdImportacion());
        comentariosNuevos += insertados;
        comentariosRepetidos += 1 - insertados;
        cambio |= insertados > 0;
      }
      if (cambio) {
        docentesAfectados++;
      }
    }
    importacion.registrarTotales(
        docentesAfectados,
        notas.getOrDefault(ResultadoRegistro.NUEVA, 0),
        notas.getOrDefault(ResultadoRegistro.CORREGIDA, 0),
        notas.getOrDefault(ResultadoRegistro.SIN_CAMBIOS, 0),
        comentariosNuevos,
        comentariosRepetidos);
  }

  private Analisis analizar(
      MultipartFile archivo, String mapeoJson, String nombrePlantilla, UsuarioSesion sesion) {
    // El mapeo y la plantilla se revisan antes que el archivo: un error ahí es de la petición (400
    // o 404), no del contenido.
    Map<String, CampoImportacion> mapeoExplicito = plantillaService.leerMapeo(mapeoJson);
    Optional<ResolutorMapeo.Plantilla> plantillaNombrada =
        mapeoExplicito == null && StringUtils.hasText(nombrePlantilla)
            ? Optional.of(plantillaService.buscarPorNombre(nombrePlantilla, sesion))
            : Optional.empty();
    byte[] contenido = leerContenido(archivo);
    Analisis analisis =
        new Analisis(nombreSeguro(archivo.getOriginalFilename()), sha256(contenido));
    TablaCsv tabla;
    try {
      tabla = lectorCsv.leer(contenido);
    } catch (ArchivoCsvInvalidoException excepcion) {
      analisis.erroresArchivo.add(ErrorFilaResponse.delArchivo(excepcion.getMessage()));
      return analisis;
    }
    analisis.codificacion = tabla.codificacion();
    analisis.delimitador = String.valueOf(tabla.delimitador());
    analisis.columnas = tabla.columnas();
    analisis.filasProcesadas = tabla.filas().size();
    if (tabla.filas().isEmpty()) {
      analisis.erroresArchivo.add(
          ErrorFilaResponse.delArchivo("El archivo solo tiene el encabezado, sin filas de datos"));
    }

    ResolutorMapeo.ResultadoMapeo mapeo =
        resolutorMapeo.resolver(
            tabla.columnas(),
            mapeoExplicito,
            plantillaNombrada,
            mapeoExplicito == null && plantillaNombrada.isEmpty()
                ? plantillaService.plantillasDe(sesion)
                : List.of());
    analisis.mapeo = mapeo.mapeo();
    analisis.origenMapeo = mapeo.origen();
    analisis.nombrePlantilla = mapeo.nombrePlantilla();
    analisis.columnasFaltantes = mapeo.columnasFaltantes();
    analisis.columnasNuevas = mapeo.columnasNuevas();
    analisis.sinMapear = mapeo.obligatoriosSinMapear();
    if (!mapeo.columnasFaltantes().isEmpty() || !mapeo.columnasNuevas().isEmpty()) {
      analisis.advertencias.add(
          "El archivo no tiene las mismas columnas que la plantilla \""
              + mapeo.nombrePlantilla()
              + "\". Faltan: "
              + listar(mapeo.columnasFaltantes())
              + ". Nuevas: "
              + listar(mapeo.columnasNuevas()));
    }
    mapeo
        .errores()
        .forEach(error -> analisis.erroresArchivo.add(ErrorFilaResponse.delArchivo(error)));
    for (CampoImportacion campo : analisis.sinMapear) {
      analisis.erroresArchivo.add(
          ErrorFilaResponse.delArchivo(
              "Ninguna columna está asignada al campo obligatorio " + campo.getEtiqueta()));
    }
    if (!analisis.sinMapear.isEmpty() || !mapeo.errores().isEmpty()) {
      return analisis;
    }
    Map<CampoImportacion, String> columnaDeCampo = invertir(analisis.mapeo);
    analisis.validacion =
        validador.validar(tabla, columnaDeCampo, buscadorDeDocentes(tabla, columnaDeCampo, sesion));
    return analisis;
  }

  /**
   * Busca de una vez todos los docentes del archivo y devuelve solo los que el usuario alcanza a
   * ver. Un docente de otra facultad queda igual que uno inexistente.
   */
  private Function<String, Optional<Usuario>> buscadorDeDocentes(
      TablaCsv tabla, Map<CampoImportacion, String> columnaDeCampo, UsuarioSesion sesion) {
    String columnaCorreo = columnaDeCampo.get(CampoImportacion.CORREO_DOCENTE);
    Set<String> correos =
        tabla.filas().stream()
            .map(fila -> fila.valor(columnaCorreo).trim().toLowerCase(Locale.ROOT))
            .filter(correo -> !correo.isEmpty())
            .collect(Collectors.toSet());
    Map<String, Usuario> visibles =
        correos.isEmpty()
            ? Map.of()
            : usuarioRepository.buscarDocentesActivosPorCorreo(correos).stream()
                .filter(
                    docente ->
                        sesion.tieneAlcanceSobreFacultad(
                            docente.getPrograma().getFacultad().getIdFacultad()))
                .collect(
                    Collectors.toMap(
                        docente -> docente.getCorreo().toLowerCase(Locale.ROOT),
                        Function.identity()));
    return correo -> Optional.ofNullable(visibles.get(correo));
  }

  private static String listar(List<String> columnas) {
    return columnas.isEmpty()
        ? "ninguna"
        : columnas.stream().map(columna -> "\"" + columna + "\"").collect(Collectors.joining(", "));
  }

  static Map<CampoImportacion, String> invertir(Map<String, CampoImportacion> mapeo) {
    Map<CampoImportacion, String> columnaDeCampo = new EnumMap<>(CampoImportacion.class);
    mapeo.forEach((columna, campo) -> columnaDeCampo.put(campo, columna));
    return columnaDeCampo;
  }

  private void avisarSiYaSeImporto(Analisis analisis, PeriodoAcademico periodo) {
    importacionRepository
        .findFirstByHashSha256AndPeriodoIdPeriodoAndResultadoOrderByIdImportacionDesc(
            analisis.hash, periodo.getIdPeriodo(), ResultadoImportacion.EXITOSA)
        .ifPresent(
            anterior ->
                analisis.advertencias.add(
                    "Este mismo archivo ya se importó en "
                        + periodo.getNombre()
                        + " (carga #"
                        + anterior.getIdImportacion()
                        + "); volver a importarlo no duplica notas ni comentarios"));
  }

  private void avisarSiLaEtapaEstaCerrada(Analisis analisis, PeriodoAcademico periodo) {
    try {
      ventanaEtapaService.exigirEtapaAbierta(periodo, TipoEtapa.RECOLECCION_CALIFICACIONES);
    } catch (EtapaCerradaException cerrada) {
      analisis.advertencias.add(cerrada.getMessage() + ": el archivo no se podrá importar hoy");
    }
  }

  private byte[] leerContenido(MultipartFile archivo) {
    if (archivo.getSize() > propiedades.tamanoMaximoBytes()) {
      throw new ArchivoDemasiadoGrandeException(
          "El archivo pesa más de " + propiedades.tamanoMaximoBytes() / 1024 + " KB");
    }
    try {
      return archivo.getBytes();
    } catch (IOException excepcion) {
      throw new UncheckedIOException("No se pudo leer el archivo subido", excepcion);
    }
  }

  private String nombreSeguro(String nombreOriginal) {
    if (!StringUtils.hasText(nombreOriginal)) {
      return "archivo.csv";
    }
    // Algunos navegadores envían la ruta completa; solo interesa el nombre.
    String nombre = Paths.get(nombreOriginal.replace('\\', '/')).getFileName().toString().trim();
    return nombre.length() > 255 ? nombre.substring(0, 255) : nombre;
  }

  private String sha256(byte[] contenido) {
    try {
      return java.util.HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
    } catch (NoSuchAlgorithmException excepcion) {
      throw new IllegalStateException("La JVM no tiene SHA-256", excepcion);
    }
  }

  /** Estado intermedio de revisar un archivo. */
  private static final class Analisis {

    private final String nombreArchivo;
    private final String hash;
    private final List<ErrorFilaResponse> erroresArchivo = new ArrayList<>();
    private final List<String> advertencias = new ArrayList<>();
    private String codificacion;
    private String delimitador;
    private List<String> columnas = List.of();
    private Map<String, CampoImportacion> mapeo = Map.of();
    private ResolutorMapeo.OrigenMapeo origenMapeo;
    private String nombrePlantilla;
    private List<String> columnasFaltantes = List.of();
    private List<String> columnasNuevas = List.of();
    private List<CampoImportacion> sinMapear = List.of();
    private int filasProcesadas;
    private ResultadoValidacion validacion;

    private Analisis(String nombreArchivo, String hash) {
      this.nombreArchivo = nombreArchivo;
      this.hash = hash;
    }

    private ReporteImportacionResponse reporte() {
      List<ErrorFilaResponse> errores = new ArrayList<>(erroresArchivo);
      List<DocenteImportacionResponse> docentes = List.of();
      int filasConError = 0;
      if (validacion != null) {
        validacion.errores().stream().map(ErrorFilaResponse::desde).forEach(errores::add);
        docentes = validacion.docentes().stream().map(DocenteImportacionResponse::desde).toList();
        filasConError = validacion.filasConError();
      }
      List<String> avisos = new ArrayList<>(advertencias);
      if (errores.size() > ERRORES_MAXIMOS_EN_REPORTE) {
        avisos.add(
            "Se muestran los primeros "
                + ERRORES_MAXIMOS_EN_REPORTE
                + " errores de "
                + errores.size());
        errores = errores.subList(0, ERRORES_MAXIMOS_EN_REPORTE);
      }
      return new ReporteImportacionResponse(
          errores.isEmpty() && validacion != null,
          nombreArchivo,
          hash,
          codificacion,
          delimitador,
          columnas,
          mapeo,
          origenMapeo,
          nombrePlantilla,
          columnasFaltantes,
          columnasNuevas,
          sinMapear,
          filasProcesadas,
          filasConError,
          List.copyOf(errores),
          docentes,
          List.copyOf(avisos));
    }
  }
}
