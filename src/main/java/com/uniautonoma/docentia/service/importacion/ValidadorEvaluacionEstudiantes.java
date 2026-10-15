package com.uniautonoma.docentia.service.importacion;

import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.Usuario;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Revisa todas las filas del CSV antes de guardar nada y agrupa lo válido por docente. No toca la
 * base: recibe la forma de buscar docentes, que ya aplica el alcance de quien carga.
 *
 * <p>Reglas por fila: el correo es obligatorio y debe ser de un docente visible; las cinco notas
 * van juntas (todas o ninguna) y cada una entre 0 y 5, con punto o coma decimal; una fila sin notas
 * es válida si trae comentario. Si un docente tiene varias filas con notas, su nota por aspecto es
 * el promedio de esas filas, redondeado a dos decimales.
 */
public class ValidadorEvaluacionEstudiantes {

  public static final int LONGITUD_MAXIMA_COMENTARIO = 2000;

  private static final BigDecimal NOTA_MINIMA = BigDecimal.ZERO;
  private static final BigDecimal NOTA_MAXIMA = new BigDecimal("5");
  private static final Pattern NUMERO = Pattern.compile("\\d+(\\.\\d+)?");

  /**
   * Valida la tabla con el mapeo de columnas dado.
   *
   * @param tabla contenido del archivo
   * @param columnaDeCampo columna del archivo asignada a cada campo; los obligatorios deben estar
   * @param buscarDocente devuelve el docente activo con ese correo (en minúsculas) solo si está
   *     dentro del alcance de quien carga
   * @return errores por fila y datos agrupados por docente
   */
  public ResultadoValidacion validar(
      TablaCsv tabla,
      Map<CampoImportacion, String> columnaDeCampo,
      Function<String, Optional<Usuario>> buscarDocente) {
    List<ErrorFila> errores = new ArrayList<>();
    Map<Long, Acumulado> porDocente = new LinkedHashMap<>();
    Map<String, Optional<Usuario>> docentesBuscados = new HashMap<>();
    Set<Integer> filasConError = new HashSet<>();

    for (TablaCsv.Fila fila : tabla.filas()) {
      List<ErrorFila> erroresFila = new ArrayList<>();
      if (fila.valoresDeMas() > 0) {
        erroresFila.add(
            new ErrorFila(
                fila.numero(),
                null,
                null,
                "La fila tiene "
                    + fila.valoresDeMas()
                    + " valores más que columnas el encabezado"));
      }
      Optional<Usuario> docente =
          validarDocente(fila, columnaDeCampo, buscarDocente, docentesBuscados, erroresFila);
      Optional<NotasAspectos> notas = validarNotas(fila, columnaDeCampo, erroresFila);
      String comentario = validarComentario(fila, columnaDeCampo, erroresFila);
      if (notas.isEmpty() && comentario == null && erroresFila.isEmpty()) {
        erroresFila.add(
            new ErrorFila(fila.numero(), null, null, "La fila no tiene notas ni comentario"));
      }

      if (!erroresFila.isEmpty()) {
        errores.addAll(erroresFila);
        filasConError.add(fila.numero());
        continue;
      }
      Acumulado acumulado =
          porDocente.computeIfAbsent(
              docente.get().getIdUsuario(), id -> new Acumulado(docente.get()));
      acumulado.filas++;
      notas.ifPresent(acumulado.notas::add);
      if (comentario != null) {
        acumulado.comentarios.add(comentario);
      }
    }

    List<DatosDocente> datos = porDocente.values().stream().map(Acumulado::cerrar).toList();
    return new ResultadoValidacion(errores, datos, tabla.filas().size(), filasConError.size());
  }

  private Optional<Usuario> validarDocente(
      TablaCsv.Fila fila,
      Map<CampoImportacion, String> columnaDeCampo,
      Function<String, Optional<Usuario>> buscarDocente,
      Map<String, Optional<Usuario>> docentesBuscados,
      List<ErrorFila> errores) {
    String columna = columnaDeCampo.get(CampoImportacion.CORREO_DOCENTE);
    String correo = fila.valor(columna).trim().toLowerCase(Locale.ROOT);
    if (correo.isEmpty()) {
      errores.add(
          new ErrorFila(fila.numero(), columna, "", "El correo del docente es obligatorio"));
      return Optional.empty();
    }
    Optional<Usuario> docente = docentesBuscados.computeIfAbsent(correo, buscarDocente);
    if (docente.isEmpty()) {
      // Mismo mensaje si no existe o si es de otra facultad, para no revelar que existe.
      errores.add(
          new ErrorFila(fila.numero(), columna, fila.valor(columna), "Docente no encontrado"));
    }
    return docente;
  }

  private Optional<NotasAspectos> validarNotas(
      TablaCsv.Fila fila, Map<CampoImportacion, String> columnaDeCampo, List<ErrorFila> errores) {
    Map<CampoImportacion, String> textos = new EnumMap<>(CampoImportacion.class);
    for (CampoImportacion aspecto : CampoImportacion.ASPECTOS) {
      textos.put(aspecto, fila.valor(columnaDeCampo.get(aspecto)).trim());
    }
    long llenas = textos.values().stream().filter(texto -> !texto.isEmpty()).count();
    if (llenas == 0) {
      return Optional.empty();
    }

    Map<CampoImportacion, BigDecimal> notas = new EnumMap<>(CampoImportacion.class);
    int erroresAntes = errores.size();
    for (CampoImportacion aspecto : CampoImportacion.ASPECTOS) {
      String columna = columnaDeCampo.get(aspecto);
      String texto = textos.get(aspecto);
      if (texto.isEmpty()) {
        errores.add(
            new ErrorFila(
                fila.numero(),
                columna,
                "",
                "Falta la nota de " + aspecto.getEtiqueta() + "; las cinco notas van juntas"));
        continue;
      }
      Optional<BigDecimal> nota = leerNumero(texto);
      if (nota.isEmpty()) {
        errores.add(new ErrorFila(fila.numero(), columna, texto, "No es un número"));
      } else if (nota.get().compareTo(NOTA_MINIMA) < 0 || nota.get().compareTo(NOTA_MAXIMA) > 0) {
        errores.add(new ErrorFila(fila.numero(), columna, texto, "La nota debe estar entre 0 y 5"));
      } else {
        notas.put(aspecto, nota.get());
      }
    }
    if (errores.size() > erroresAntes) {
      return Optional.empty();
    }
    return Optional.of(
        new NotasAspectos(
            notas.get(CampoImportacion.DOMINIO_DISCIPLINAR),
            notas.get(CampoImportacion.PLANEACION_ACADEMICA),
            notas.get(CampoImportacion.METODOLOGIA_ENSENANZA),
            notas.get(CampoImportacion.EVALUACION_APRENDIZAJE),
            notas.get(CampoImportacion.COMPROMISO_INSTITUCIONAL)));
  }

  private String validarComentario(
      TablaCsv.Fila fila, Map<CampoImportacion, String> columnaDeCampo, List<ErrorFila> errores) {
    String columna = columnaDeCampo.get(CampoImportacion.COMENTARIO);
    String comentario = fila.valor(columna).trim();
    if (comentario.isEmpty()) {
      return null;
    }
    if (comentario.length() > LONGITUD_MAXIMA_COMENTARIO) {
      errores.add(
          new ErrorFila(
              fila.numero(),
              columna,
              comentario.substring(0, 50) + "…",
              "El comentario supera los " + LONGITUD_MAXIMA_COMENTARIO + " caracteres"));
      return null;
    }
    return comentario;
  }

  /** Acepta 4, 4.5 y 4,5; rechaza signos, separadores de miles y texto. */
  static Optional<BigDecimal> leerNumero(String texto) {
    String normalizado = texto.indexOf('.') < 0 ? texto.replace(',', '.') : texto;
    if (!NUMERO.matcher(normalizado).matches()) {
      return Optional.empty();
    }
    return Optional.of(new BigDecimal(normalizado));
  }

  /**
   * Huella de un comentario para la clave de idempotencia: texto con los espacios normalizados más
   * su número de aparición para ese docente dentro del archivo.
   */
  public static String huellaComentario(String texto, int aparicion) {
    String normalizado = normalizarComentario(texto);
    try {
      byte[] huella =
          MessageDigest.getInstance("SHA-256")
              .digest((normalizado + "\u0000" + aparicion).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(huella);
    } catch (NoSuchAlgorithmException excepcion) {
      throw new IllegalStateException("La JVM no tiene SHA-256", excepcion);
    }
  }

  /** Mismo texto aunque cambien los espacios o la forma de escribir las tildes. */
  public static String normalizarComentario(String texto) {
    return Normalizer.normalize(texto, Normalizer.Form.NFC).trim().replaceAll("\\s+", " ");
  }

  /** Un problema en una celda o en una fila completa (columna nula). */
  public record ErrorFila(Integer fila, String columna, String valor, String motivo) {}

  /**
   * Lo que el archivo trae para un docente.
   *
   * @param notas promedio por aspecto de sus filas con notas, o nulo si solo trae comentarios
   * @param comentarios en el orden del archivo
   */
  public record DatosDocente(
      Usuario docente,
      NotasAspectos notas,
      int filas,
      int filasConNotas,
      List<String> comentarios) {}

  public record ResultadoValidacion(
      List<ErrorFila> errores,
      List<DatosDocente> docentes,
      int filasProcesadas,
      int filasConError) {

    public boolean esValido() {
      return errores.isEmpty();
    }
  }

  private static final class Acumulado {

    private final Usuario docente;
    private final List<NotasAspectos> notas = new ArrayList<>();
    private final List<String> comentarios = new ArrayList<>();
    private int filas;

    private Acumulado(Usuario docente) {
      this.docente = docente;
    }

    private DatosDocente cerrar() {
      return new DatosDocente(docente, promediar(), filas, notas.size(), List.copyOf(comentarios));
    }

    private NotasAspectos promediar() {
      if (notas.isEmpty()) {
        return null;
      }
      return new NotasAspectos(
          promedio(NotasAspectos::dominioDisciplinar),
          promedio(NotasAspectos::planeacionAcademica),
          promedio(NotasAspectos::metodologiaEnsenanza),
          promedio(NotasAspectos::evaluacionAprendizaje),
          promedio(NotasAspectos::compromisoInstitucional));
    }

    private BigDecimal promedio(Function<NotasAspectos, BigDecimal> aspecto) {
      BigDecimal suma = notas.stream().map(aspecto).reduce(BigDecimal.ZERO, BigDecimal::add);
      return suma.divide(BigDecimal.valueOf(notas.size()), 2, RoundingMode.HALF_UP);
    }
  }
}
