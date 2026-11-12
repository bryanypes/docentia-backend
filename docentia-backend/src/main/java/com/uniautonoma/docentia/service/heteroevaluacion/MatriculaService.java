package com.uniautonoma.docentia.service.heteroevaluacion;

import com.uniautonoma.docentia.config.HeteroevaluacionProperties;
import com.uniautonoma.docentia.dto.ResultadoMatriculaResponse;
import com.uniautonoma.docentia.dto.ResultadoMatriculaResponse.ErrorFila;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.ArchivoDemasiadoGrandeException;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import com.uniautonoma.docentia.service.importacion.LectorCsv;
import com.uniautonoma.docentia.service.importacion.TablaCsv;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Carga la matrícula del periodo activo por CSV. Todo o nada e idempotente: reimportar no duplica.
 * Crea los estudiantes que falten, sin una contraseña utilizable fuera de desarrollo.
 */
@Service
@Transactional
public class MatriculaService {

  public static final List<String> COLUMNAS =
      List.of(
          "correo_estudiante",
          "nombre_estudiante",
          "programa",
          "codigo_curso",
          "nombre_curso",
          "correo_docente");
  public static final String COLUMNA_ESTADO = "estado";
  public static final String PLANTILLA =
      String.join(",", COLUMNAS)
          + ",estado\n"
          + "ana.perez@uniautonoma.edu.co,Ana Pérez,Ingeniería de Software,ISW-101,Programación"
          + " web,docente@uniautonoma.edu.co,ACTIVA\n";

  private static final String DOMINIO = "@uniautonoma.edu.co";
  private static final String GRUPO = "UNICO";
  private static final String JORNADA = "UNICA";
  private static final String TIPO = "CSV";

  private final JdbcTemplate jdbcTemplate;
  private final UsuarioRepository usuarioRepository;
  private final ProgramaRepository programaRepository;
  private final PeriodoAcademicoService periodoService;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher eventos;
  private final HeteroevaluacionProperties propiedades;
  private final String contrasenaDesarrollo;

  public MatriculaService(
      JdbcTemplate jdbcTemplate,
      UsuarioRepository usuarioRepository,
      ProgramaRepository programaRepository,
      PeriodoAcademicoService periodoService,
      PasswordEncoder passwordEncoder,
      ApplicationEventPublisher eventos,
      HeteroevaluacionProperties propiedades,
      @Value("${docentia.desarrollo.contrasena-inicial:}") String contrasenaDesarrollo) {
    this.jdbcTemplate = jdbcTemplate;
    this.usuarioRepository = usuarioRepository;
    this.programaRepository = programaRepository;
    this.periodoService = periodoService;
    this.passwordEncoder = passwordEncoder;
    this.eventos = eventos;
    this.propiedades = propiedades;
    this.contrasenaDesarrollo = contrasenaDesarrollo;
  }

  private record Linea(
      int fila,
      String correo,
      String nombre,
      Programa programa,
      String codigoCurso,
      String nombreCurso,
      Usuario docente,
      boolean retirada) {}

  public ResultadoMatriculaResponse importar(MultipartFile archivo, UsuarioSesion sesion) {
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    TablaCsv tabla = leer(archivo);
    List<ErrorFila> errores = new ArrayList<>();
    for (String columna : COLUMNAS) {
      if (!tabla.columnas().contains(columna)) {
        errores.add(new ErrorFila(1, "Falta la columna " + columna));
      }
    }
    List<Linea> lineas = new ArrayList<>();
    if (errores.isEmpty()) {
      Map<String, Usuario> usuarios = new HashMap<>();
      for (TablaCsv.Fila fila : tabla.filas()) {
        validar(fila, sesion, usuarios, errores).ifPresent(lineas::add);
      }
    }
    if (!errores.isEmpty()) {
      avisar(sesion, false);
      return new ResultadoMatriculaResponse(false, tabla.filas().size(), errores, 0, 0, 0);
    }
    int[] conteos = new int[3];
    for (Linea linea : lineas) {
      escribir(linea, periodo, conteos);
    }
    avisar(sesion, true);
    return new ResultadoMatriculaResponse(
        true, lineas.size(), List.of(), conteos[0], conteos[1], conteos[2]);
  }

  private TablaCsv leer(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new DatosInvalidosException("Adjunta el archivo CSV", "archivo");
    }
    String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
    if (!nombre.toLowerCase(Locale.ROOT).endsWith(".csv")) {
      throw new DatosInvalidosException("El archivo debe ser .csv", "archivo");
    }
    if (archivo.getSize() > propiedades.tamanoMaximoMatriculaBytes()) {
      throw new ArchivoDemasiadoGrandeException(
          "El archivo pesa más de "
              + propiedades.tamanoMaximoMatriculaBytes() / (1024 * 1024)
              + " MB");
    }
    try {
      TablaCsv tabla = new LectorCsv(propiedades.filasMaximasMatricula()).leer(archivo.getBytes());
      List<String> columnas =
          tabla.columnas().stream().map(c -> c.trim().toLowerCase(Locale.ROOT)).toList();
      List<TablaCsv.Fila> filas =
          tabla.filas().stream()
              .map(
                  fila -> {
                    Map<String, String> valores = new LinkedHashMap<>();
                    fila.valores()
                        .forEach((k, v) -> valores.put(k.trim().toLowerCase(Locale.ROOT), v));
                    return new TablaCsv.Fila(fila.numero(), valores, fila.valoresDeMas());
                  })
              .toList();
      return new TablaCsv(columnas, filas, tabla.codificacion(), tabla.delimitador());
    } catch (IOException fallo) {
      throw new DatosInvalidosException("No se pudo leer el archivo", "archivo");
    }
  }

  private Optional<Linea> validar(
      TablaCsv.Fila fila,
      UsuarioSesion sesion,
      Map<String, Usuario> usuarios,
      List<ErrorFila> errores) {
    int numero = fila.numero();
    String correo = normalizar(fila.valor("correo_estudiante"));
    String nombre = fila.valor("nombre_estudiante").trim();
    String nombrePrograma = fila.valor("programa").trim();
    String codigoCurso = fila.valor("codigo_curso").trim();
    String nombreCurso = fila.valor("nombre_curso").trim();
    String correoDocente = normalizar(fila.valor("correo_docente"));
    String estado = fila.valor(COLUMNA_ESTADO).trim().toUpperCase(Locale.ROOT);
    int antes = errores.size();
    if (!correo.endsWith(DOMINIO)) {
      errores.add(new ErrorFila(numero, "El correo del estudiante debe ser institucional"));
    }
    if (nombre.isEmpty() || nombre.length() > 150) {
      errores.add(new ErrorFila(numero, "El nombre del estudiante es obligatorio (máx. 150)"));
    }
    if (codigoCurso.isEmpty() || codigoCurso.length() > 40) {
      errores.add(new ErrorFila(numero, "El código del curso es obligatorio (máx. 40)"));
    }
    if (nombreCurso.isEmpty() || nombreCurso.length() > 200) {
      errores.add(new ErrorFila(numero, "El nombre del curso es obligatorio (máx. 200)"));
    }
    if (!estado.isEmpty() && !estado.equals("ACTIVA") && !estado.equals("RETIRADA")) {
      errores.add(new ErrorFila(numero, "El estado debe ser ACTIVA o RETIRADA"));
    }
    Optional<Programa> programa =
        programaRepository
            .findByCodigoIgnoreCaseAndEsActivoTrue(nombrePrograma)
            .or(
                () ->
                    programaRepository.findFirstByNombreIgnoreCaseAndEsActivoTrue(nombrePrograma));
    if (programa.isEmpty()) {
      errores.add(new ErrorFila(numero, "El programa no existe"));
    }
    Usuario docente = buscar(correoDocente, usuarios);
    if (docente == null || docente.getRol() != Rol.DOCENTE || !docente.getEsActivo()) {
      errores.add(new ErrorFila(numero, "El docente no existe o no está activo"));
    } else if (sesion.esCoordinador()
        && !docente.getPrograma().getFacultad().getIdFacultad().equals(sesion.idFacultad())) {
      errores.add(new ErrorFila(numero, "El docente no es de tu facultad"));
    }
    Usuario estudiante = buscar(correo, usuarios);
    if (estudiante != null && estudiante.getRol() != Rol.ESTUDIANTE) {
      errores.add(new ErrorFila(numero, "El correo pertenece a un usuario que no es estudiante"));
    }
    if (errores.size() > antes) {
      return Optional.empty();
    }
    return Optional.of(
        new Linea(
            numero,
            correo,
            nombre,
            programa.get(),
            codigoCurso,
            nombreCurso,
            docente,
            estado.equals("RETIRADA")));
  }

  private Usuario buscar(String correo, Map<String, Usuario> cache) {
    if (correo.isEmpty()) {
      return null;
    }
    return cache.computeIfAbsent(
        correo, c -> usuarioRepository.findByCorreoIgnoreCase(c).orElse(null));
  }

  private void escribir(Linea linea, PeriodoAcademico periodo, int[] conteos) {
    Usuario estudiante =
        usuarioRepository
            .findByCorreoIgnoreCase(linea.correo())
            .orElseGet(
                () -> {
                  conteos[0]++;
                  return usuarioRepository.saveAndFlush(
                      Usuario.crearEstudiante(
                          linea.nombre(), linea.correo(), hashInicial(), linea.programa()));
                });
    Programa programaCurso = linea.docente().getPrograma();
    jdbcTemplate.update(
        "insert into cursos (codigo, nombre, id_programa) values (?, ?, ?)"
            + " on conflict (codigo) do nothing",
        linea.codigoCurso(),
        linea.nombreCurso(),
        programaCurso.getIdPrograma());
    Long idCurso =
        jdbcTemplate.queryForObject(
            "select id from cursos where lower(codigo) = lower(?)",
            Long.class,
            linea.codigoCurso());
    jdbcTemplate.update(
        "insert into clases (id_periodo, id_curso, grupo, jornada, tipo_curso) values (?, ?, ?, ?, ?)"
            + " on conflict do nothing",
        periodo.getIdPeriodo(),
        idCurso,
        GRUPO,
        JORNADA,
        TIPO);
    Long idClase =
        jdbcTemplate.queryForObject(
            "select id from clases where id_periodo = ? and id_curso = ? and grupo = ? and jornada = ?",
            Long.class,
            periodo.getIdPeriodo(),
            idCurso,
            GRUPO,
            JORNADA);
    jdbcTemplate.update(
        "insert into clases_docente (clase_id, docente_id) values (?, ?) on conflict do nothing",
        idClase,
        linea.docente().getIdUsuario());
    if (linea.retirada()) {
      conteos[2] +=
          jdbcTemplate.update(
              "update matriculas set es_activa = false"
                  + " where clase_id = ? and estudiante_id = ? and es_activa",
              idClase,
              estudiante.getIdUsuario());
    } else {
      conteos[1] +=
          jdbcTemplate.update(
              "insert into matriculas (clase_id, estudiante_id) select ?, ?"
                  + " where not exists (select 1 from matriculas"
                  + " where clase_id = ? and estudiante_id = ? and es_activa)",
              idClase,
              estudiante.getIdUsuario(),
              idClase,
              estudiante.getIdUsuario());
    }
  }

  /** En desarrollo, la contraseña de demo; fuera de él, ninguna utilizable: entra con Google. */
  private String hashInicial() {
    return passwordEncoder.encode(
        contrasenaDesarrollo.isBlank()
            ? UUID.randomUUID().toString() + UUID.randomUUID()
            : contrasenaDesarrollo);
  }

  private static String normalizar(String correo) {
    return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
  }

  private void avisar(UsuarioSesion sesion, boolean exitosa) {
    eventos.publishEvent(
        EventoNotificacion.aUsuario(
            exitosa
                ? TipoNotificacion.IMPORTACION_EXITOSA
                : TipoNotificacion.IMPORTACION_CON_ERRORES,
            sesion.idUsuario(),
            "/notificaciones",
            "MATRICULA",
            null,
            "matricula:" + UUID.randomUUID()));
  }
}
