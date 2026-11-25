package com.uniautonoma.docentia.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.CategoriaDesempeno;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.DatosCongeladosActa;
import com.uniautonoma.docentia.entity.EstadoHallazgo;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FirmaActa;
import com.uniautonoma.docentia.entity.Hallazgo;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.entity.TipoActa;
import com.uniautonoma.docentia.entity.TipoHallazgo;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Arma el PDF de un acta firmada. Lleva el resultado final, nunca las notas por fuente (A4), porque
 * el docente lo descarga. El hash impreso es el del contenido; el del archivo se guarda aparte.
 *
 * <p>El logo institucional se lee de {@code docentia.actas.logo}; si el archivo no existe, el acta
 * sale sin logo.
 */
@Component
public class GeneradorPdfActa {

  private static final Locale ESPANOL = Locale.forLanguageTag("es-CO");
  private static final DateTimeFormatter FORMATO_FECHA =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm '(hora de Colombia)'", ESPANOL);
  private static final DateTimeFormatter FECHA_LARGA =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ESPANOL);
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ESPANOL);
  private static final String CIUDAD = "Popayán";
  private static final Color AZUL = new Color(5, 54, 134);
  private static final Color DORADO = new Color(184, 150, 62);
  private static final String UNIVERSIDAD = "Corporación Universitaria Autónoma del Cauca";
  private static final Color BORDE = new Color(214, 206, 188);
  private static final Color TENUE = new Color(102, 98, 90);

  private final byte[] logo;
  private final String urlVerificacion;
  private final SolicitudAnalisisRepository analisis;

  public GeneradorPdfActa(
      ResourceLoader recursos,
      @Value("${docentia.actas.logo:classpath:marca/logo-uniautonoma.png}") String rutaLogo,
      @Value("${docentia.actas.url-verificacion:https://nova.uniautonoma.edu.co/Evadocente-app}")
          String urlPublica,
      SolicitudAnalisisRepository analisis) {
    this.analisis = analisis;
    this.logo = leerLogo(recursos.getResource(rutaLogo));
    this.urlVerificacion = urlPublica.replaceAll("/+$", "") + "/verificar-acta/";
  }

  private static byte[] leerLogo(Resource recurso) {
    if (!recurso.exists()) {
      return null;
    }
    try (InputStream entrada = recurso.getInputStream()) {
      return entrada.readAllBytes();
    } catch (IOException ilegible) {
      return null;
    }
  }

  /** Texto canónico del acta: lo que el hash de contenido garantiza. */
  public String contenidoCanonico(Acta acta, String codigo) {
    String compromisos =
        acta.getCompromisos().stream()
            .map(c -> c.getDescripcion() + "|" + c.getFechaLimite())
            .collect(Collectors.joining(";"));
    String firmas =
        acta.getFirmas().stream()
            .map(
                f ->
                    f.getRol()
                        + "|"
                        + f.getNombre()
                        + "|"
                        + f.getFecha()
                        + "|"
                        + f.getIp()
                        + (f.getHashImagen() == null ? "" : "|" + f.getHashImagen()))
            .collect(Collectors.joining(";"));
    return String.join(
            "\n",
            codigo,
            acta.getDocente().getCorreo(),
            acta.getPeriodo().getNombre(),
            String.valueOf(acta.getVersion()),
            String.valueOf(acta.getPromedio()),
            String.valueOf(acta.getCategoria()),
            compromisos,
            firmas)
        // Solo si existen: las actas firmadas antes de este campo conservan su hash.
        + (acta.getObservaciones() == null ? "" : "\n" + acta.getObservaciones())
        + "\n"
        + datosCanonicos(acta.getDatosCongelados());
  }

  /** Identificación, reunión, pesos y aspectos conversados que el acta congeló (V51). */
  private static String datosCanonicos(DatosCongeladosActa d) {
    return String.join(
        "|",
        String.valueOf(d.docenteNombre()),
        String.valueOf(d.docenteCorreo()),
        String.valueOf(d.documentoRotulo()),
        String.valueOf(d.docenteDocumento()),
        String.valueOf(d.docenteCargo()),
        String.valueOf(d.programaNombre()),
        String.valueOf(d.escuelaNombre()),
        String.valueOf(d.periodoNombre()),
        String.valueOf(d.periodoInicio()),
        String.valueOf(d.periodoFin()),
        String.valueOf(d.evaluadorCargo()),
        String.valueOf(d.reunionFecha()),
        String.valueOf(d.reunionHoraInicio()),
        String.valueOf(d.reunionHoraFin()),
        String.valueOf(d.reunionAsistencia()),
        String.valueOf(d.pesoEstudiantes()),
        String.valueOf(d.pesoCoordinacion()),
        String.valueOf(d.pesoAutoevaluacion()),
        String.valueOf(d.respuestasEstudiantes()),
        String.join(";", d.fortalezas()),
        String.join(";", d.aspectosPorMejorar()),
        String.valueOf(d.formatoCodigo()),
        String.valueOf(d.formatoVersion()));
  }

  public String sha256(byte[] datos) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(datos));
    } catch (NoSuchAlgorithmException excepcion) {
      throw new IllegalStateException("SHA-256 no está disponible", excepcion);
    }
  }

  public String hashContenido(Acta acta, String codigo) {
    return sha256(contenidoCanonico(acta, codigo).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Genera el documento.
   *
   * @param acta acta con sus dos firmas
   * @param codigo código de verificación
   * @param hashContenido hash SHA-256 del contenido
   * @return bytes del PDF
   */
  public byte[] generar(Acta acta, String codigo, String hashContenido) {
    Font titulo = FontFactory.getFont(FontFactory.TIMES_BOLD, 15, AZUL);
    Font seccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, AZUL);
    Font texto = FontFactory.getFont(FontFactory.TIMES_ROMAN, 11.5f);
    Font fuerte = FontFactory.getFont(FontFactory.TIMES_BOLD, 11.5f);
    Font cursiva = FontFactory.getFont(FontFactory.TIMES_ITALIC, 11.5f, TENUE);
    Font rotulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, TENUE);
    Font pequena = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, TENUE);
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    boolean compromiso = acta.getTipo() == TipoActa.COMPROMISO;
    String nombreActa =
        compromiso ? "Acta de compromiso docente" : "Acta de retroalimentación docente";
    // Márgenes de documento oficial; el membrete y el pie los dibuja Membrete en cada página.
    Document documento = new Document(PageSize.LETTER, 72, 72, 124, 74);
    try {
      PdfWriter escritor = PdfWriter.getInstance(documento, salida);
      // Al justificar se estiran los espacios, no las letras.
      escritor.setSpaceCharRatio(PdfWriter.NO_SPACE_CHAR_RATIO);
      DatosCongeladosActa datos = acta.getDatosCongelados();
      escritor.setPageEvent(new Membrete(logo, codigo, urlVerificacion, formato(datos)));
      documento.open();
      documento.addTitle(nombreActa);
      documento.addAuthor(UNIVERSIDAD);

      Paragraph encabezado = new Paragraph(nombreActa.toUpperCase(ESPANOL), titulo);
      encabezado.setAlignment(Element.ALIGN_CENTER);
      documento.add(encabezado);
      Paragraph periodo =
          new Paragraph("Evaluación docente · Periodo académico " + nombrePeriodo(acta), cursiva);
      periodo.setAlignment(Element.ALIGN_CENTER);
      periodo.setSpacingAfter(16);
      documento.add(periodo);
      documento.add(identificacion(acta, datos, rotulo, cargoFuente()));

      Paragraph lugar =
          new Paragraph(CIUDAD + ", " + fechaExpedicion(acta).format(FECHA_LARGA), texto);
      lugar.setSpacingAfter(14);
      documento.add(lugar);

      String nombreDocente = nombreDocente(acta);
      String evaluador = nombreDeQuienEvaluo(acta);
      documento.add(
          parrafo(
              texto,
              fuerte,
              aperturaReunion(acta),
              "Participaron ",
              "**" + nombreDocente + "**",
              ", " + presentacionDelEvaluado(acta) + ", y ",
              "**" + evaluador + "**",
              ", en calidad de "
                  + minusculaInicial(cargoDelFirmante(acta, acta.rolEvaluador()))
                  + ", con el propósito de revisar los resultados de su evaluación docente y"
                  + " acordar las acciones que se derivan de ella."));

      int numero = 1;
      documento.add(subtitulo(numero++, "Resultado de la evaluación", seccion));
      documento.add(
          parrafo(
              texto,
              fuerte,
              "Con las calificaciones de los estudiantes, de "
                  + evaluadorDe(acta)
                  + " y la autoevaluación, ponderadas según la Resolución 074"
                  + ponderacion(acta, datos)
                  + ", la evaluación arrojó una nota final de ",
              "**" + acta.getPromedio().toPlainString().replace('.', ',') + " sobre 5,00**",
              ", que corresponde a la categoría ",
              "**" + acta.getCategoria().getEtiqueta() + "**",
              ". La acción institucional que corresponde a esta categoría es: "
                  + minusculaInicial(acta.getCategoria().getAccionInstitucional())
                  + "."));

      List<String> fortalezas = datos.fortalezas();
      List<String> mejoras = datos.aspectosPorMejorar();
      if (!fortalezas.isEmpty() || !mejoras.isEmpty()) {
        documento.add(subtitulo(numero++, "Aspectos conversados", seccion));
        if (!fortalezas.isEmpty()) {
          documento.add(
              parrafo(
                  texto,
                  fuerte,
                  "En la reunión se destacaron las siguientes fortalezas, identificadas en los"
                      + " comentarios de los estudiantes:"));
          vinetas(documento, fortalezas, texto);
        }
        if (!mejoras.isEmpty()) {
          documento.add(
              parrafo(
                  texto,
                  fuerte,
                  "Asimismo, se conversó sobre los siguientes aspectos por mejorar:"));
          vinetas(documento, mejoras, texto);
        }
      }
      if (acta.getObservaciones() != null) {
        documento.add(subtitulo(numero++, "Desarrollo de la reunión", seccion));
        documento.add(parrafo(texto, fuerte, acta.getObservaciones()));
      }

      documento.add(
          subtitulo(
              numero,
              compromiso && acta.getCategoria() == CategoriaDesempeno.BAJO
                  ? "Plan de Mejoramiento Profesoral"
                  : "Compromisos",
              seccion));
      if (acta.getCompromisos().isEmpty()) {
        documento.add(
            parrafo(
                texto,
                fuerte,
                "Dado el resultado, no se establecieron compromisos de mejora en esta reunión y se"
                    + " invita a mantener las prácticas que fueron reconocidas."));
      } else {
        documento.add(
            parrafo(
                texto,
                fuerte,
                "Como resultado de la reunión, ",
                "**" + nombreDocente + "**",
                " se compromete a:"));
        char literal = 'a';
        for (Compromiso c : acta.getCompromisos()) {
          Paragraph item =
              parrafo(
                  texto,
                  fuerte,
                  literal++ + ")  " + finConPunto(c.getDescripcion()) + " ",
                  "**Plazo: " + c.getFechaLimite().format(FECHA_LARGA) + ".**");
          item.setIndentationLeft(32);
          item.setFirstLineIndent(-16);
          documento.add(item);
        }
      }

      Paragraph cierre =
          parrafo(
              texto,
              fuerte,
              "Para constancia, se firma electrónicamente la presente acta en "
                  + CIUDAD
                  + ", el "
                  + fechaExpedicion(acta).format(FECHA_LARGA)
                  + ".");
      // La fórmula de cierre va dentro del bloque de firmas: nunca queda sola en otra página.
      PdfPTable bloqueFirmas = firmas(acta, cierre, fuerte, rotulo, pequena);
      bloqueFirmas.setKeepTogether(true);
      documento.add(bloqueFirmas);

      Paragraph verificacion = new Paragraph();
      verificacion.setSpacingBefore(12);
      verificacion.setLeading(10);
      verificacion.add(new Chunk("Constancia de firma electrónica. ", rotulo));
      verificacion.add(
          new Chunk(
              "Las firmas se registraron con la identidad de cada cuenta institucional, la fecha y"
                  + " el trazo; no incluyen certificado digital criptográfico. Huella SHA-256 del"
                  + " contenido: "
                  + hashContenido
                  + ".",
              pequena));
      documento.add(verificacion);
      documento.close();
    } catch (DocumentException excepcion) {
      throw new IllegalStateException("No se pudo generar el PDF del acta", excepcion);
    }
    return salida.toByteArray();
  }

  /** Párrafo justificado. Los fragmentos entre ** van en negrita; los demás, en el texto normal. */
  private static Paragraph parrafo(Font texto, Font fuerte, String... fragmentos) {
    Paragraph parrafo = new Paragraph();
    parrafo.setAlignment(Element.ALIGN_JUSTIFIED);
    parrafo.setLeading(16);
    parrafo.setSpacingAfter(8);
    for (String fragmento : fragmentos) {
      boolean negrita = fragmento.startsWith("**") && fragmento.endsWith("**");
      parrafo.add(
          new Chunk(
              negrita ? fragmento.substring(2, fragmento.length() - 2) : fragmento,
              negrita ? fuerte : texto));
    }
    return parrafo;
  }

  /** Un párrafo por viñeta: así todas comparten la misma sangría francesa. */
  private static void vinetas(Document documento, List<String> lineas, Font texto)
      throws DocumentException {
    for (int i = 0; i < lineas.size(); i++) {
      Paragraph item = new Paragraph("•   " + finConPunto(lineas.get(i)), texto);
      item.setLeading(16);
      item.setIndentationLeft(32);
      item.setFirstLineIndent(-12);
      item.setSpacingAfter(i == lineas.size() - 1 ? 8 : 2);
      documento.add(item);
    }
  }

  /** "En Popayán, el 19 de noviembre de 2026, entre las 22:00 y las 22:30, se llevó a cabo…" */
  private static String aperturaReunion(Acta acta) {
    DatosCongeladosActa datos = acta.getDatosCongelados();
    String motivo =
        " la reunión de retroalimentación de la evaluación docente del periodo académico "
            + nombrePeriodo(acta)
            + ", conforme a la Resolución 074. ";
    if (datos.reunionFecha() == null) {
      return "Se llevó a cabo" + motivo;
    }
    return "En "
        + CIUDAD
        + ", el "
        + datos.reunionFecha().format(FECHA_LARGA)
        + ", entre las "
        + datos.reunionHoraInicio().format(HORA)
        + " y las "
        + datos.reunionHoraFin().format(HORA)
        + ", se llevó a cabo"
        + motivo;
  }

  /**
   * "docente del programa Ingeniería de Software, de la Escuela de Ingeniería", con los datos que
   * el acta congeló.
   */
  private static String presentacionDelEvaluado(Acta acta) {
    DatosCongeladosActa datos = acta.getDatosCongelados();
    if (datos.programaNombre() != null) {
      return "docente del programa "
          + datos.programaNombre()
          + ", de la "
          + (datos.escuelaNombre() == null ? "No aplica" : datos.escuelaNombre());
    }
    return minusculaInicial(cargoDelFirmante(acta, Rol.DOCENTE));
  }

  private static String nombreDocente(Acta acta) {
    String congelado = acta.getDatosCongelados().docenteNombre();
    return congelado != null ? congelado : acta.getDocente().getNombreCompleto();
  }

  private static String nombrePeriodo(Acta acta) {
    String congelado = acta.getDatosCongelados().periodoNombre();
    return congelado != null ? congelado : acta.getPeriodo().getNombre();
  }

  /** " (estudiantes 50 %, coordinación 30 % y autoevaluación 20 %)", si el acta tiene los pesos. */
  private static String ponderacion(Acta acta, DatosCongeladosActa datos) {
    if (datos.pesoEstudiantes() == null) {
      return "";
    }
    return " (estudiantes "
        + porcentaje(datos.pesoEstudiantes())
        + ", "
        + (acta.rolEvaluador() == Rol.VICERRECTOR ? "vicerrectoría " : "coordinación ")
        + porcentaje(datos.pesoCoordinacion())
        + " y autoevaluación "
        + porcentaje(datos.pesoAutoevaluacion())
        + ")";
  }

  private static String porcentaje(BigDecimal peso) {
    return peso.stripTrailingZeros().toPlainString().replace('.', ',') + " %";
  }

  /** "Formato F-VA-012 · Versión 1", o nulo si la institución aún no lo definió. */
  private static String formato(DatosCongeladosActa datos) {
    if (datos.formatoCodigo() == null) {
      return null;
    }
    return "Formato "
        + datos.formatoCodigo()
        + (datos.formatoVersion() == null ? "" : " · Versión " + datos.formatoVersion());
  }

  private static Font cargoFuente() {
    return FontFactory.getFont(FontFactory.TIMES_ROMAN, 10.5f);
  }

  /** Cuadro de identificación: docente, documento, cargo, programa, escuela, periodo y reunión. */
  private static PdfPTable identificacion(
      Acta acta, DatosCongeladosActa datos, Font rotulo, Font valor) {
    PdfPTable tabla = new PdfPTable(new float[] {1.25f, 3f});
    tabla.setWidthPercentage(100);
    tabla.setSpacingAfter(16);
    fila(tabla, "Docente", nombreDocente(acta), rotulo, valor);
    if (datos.documentoRotulo() != null && datos.docenteDocumento() != null) {
      fila(tabla, datos.documentoRotulo(), datos.docenteDocumento(), rotulo, valor);
    }
    fila(tabla, "Cargo", cargoDelFirmante(acta, Rol.DOCENTE), rotulo, valor);
    if (datos.programaNombre() != null) {
      fila(tabla, "Programa", datos.programaNombre(), rotulo, valor);
    }
    if (datos.escuelaNombre() != null) {
      fila(tabla, "Escuela", datos.escuelaNombre(), rotulo, valor);
    }
    String periodo = nombrePeriodo(acta);
    if (datos.periodoInicio() != null && datos.periodoFin() != null) {
      periodo +=
          " (del "
              + datos.periodoInicio().format(FECHA_LARGA)
              + " al "
              + datos.periodoFin().format(FECHA_LARGA)
              + ")";
    }
    fila(tabla, "Periodo académico", periodo, rotulo, valor);
    fila(tabla, "Evaluó", cargoDelFirmante(acta, acta.rolEvaluador()), rotulo, valor);
    if (datos.reunionFecha() != null) {
      fila(
          tabla,
          "Reunión",
          datos.reunionFecha().format(FECHA_LARGA)
              + ", de "
              + datos.reunionHoraInicio().format(HORA)
              + " a "
              + datos.reunionHoraFin().format(HORA),
          rotulo,
          valor);
    }
    return tabla;
  }

  private static void fila(PdfPTable tabla, String rotulo, String valor, Font fr, Font fv) {
    PdfPCell nombre = new PdfPCell(new Phrase(rotulo.toUpperCase(ESPANOL), fr));
    PdfPCell dato = new PdfPCell(new Phrase(valor, fv));
    for (PdfPCell celda : List.of(nombre, dato)) {
      celda.setBorder(Rectangle.BOTTOM);
      celda.setBorderColor(BORDE);
      celda.setBorderWidth(0.6f);
      celda.setPaddingTop(3);
      celda.setPaddingBottom(5);
      celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
      tabla.addCell(celda);
    }
  }

  /** Nombre de quien firmó como evaluador; el acta se genera con las dos firmas. */
  private static String nombreDeQuienEvaluo(Acta acta) {
    Rol evaluador = acta.rolEvaluador();
    return acta.getFirmas().stream()
        .filter(f -> f.getRol() == evaluador)
        .map(FirmaActa::getNombre)
        .findFirst()
        .orElse(evaluador == Rol.VICERRECTOR ? "la vicerrectoría" : "la coordinación");
  }

  /**
   * Hallazgos validados y publicados del análisis de comentarios del periodo, en el texto con que
   * van al acta. El acta los congela; el PDF imprime esa copia.
   */
  public List<String> hallazgosValidados(Long idDocente, Long idPeriodo, TipoHallazgo tipo) {
    Optional<SolicitudAnalisis> publicada =
        analisis
            .findFirstByDocenteIdUsuarioAndPeriodoIdPeriodoAndEstadoBorradorOrderByIdSolicitudDesc(
                idDocente, idPeriodo, "VALIDADO");
    return publicada.map(SolicitudAnalisis::getHallazgos).orElse(List.of()).stream()
        .filter(h -> h.getEstado() == EstadoHallazgo.VALIDADO && h.getTipo() == tipo)
        .map(Hallazgo::getTexto)
        // "Fortaleza mencionada con frecuencia: X" → "X": en la carta el contexto ya lo dice.
        .map(t -> t.replaceFirst("^[^:]{0,60}mencionada con frecuencia:\\s*", ""))
        // Ni el conteo "(4 de 14 comentarios)": el acta no cuantifica a los estudiantes.
        .map(t -> t.replaceFirst("\\s*\\(\\s*\\d+\\s+de\\s+\\d+\\s+comentarios?\\s*\\)", ""))
        .map(t -> t.isEmpty() ? t : t.substring(0, 1).toUpperCase(ESPANOL) + t.substring(1))
        .toList();
  }

  private static String finConPunto(String texto) {
    String limpio = texto.strip();
    return limpio.endsWith(".") ? limpio : limpio + ".";
  }

  private static String minusculaInicial(String texto) {
    return texto.isEmpty()
        ? texto
        : texto.substring(0, 1).toLowerCase(ESPANOL) + texto.substring(1);
  }

  /**
   * En la universidad la unidad académica se llama escuela: "Facultad de Ingeniería" e "Ingeniería"
   * pasan a "Escuela de Ingeniería".
   */
  static String nombreEscuela(Facultad facultad) {
    if (facultad == null) {
      return "No aplica";
    }
    String nombre =
        facultad.getNombre().strip().replaceFirst("(?i)^(facultad|escuela)\\s+de\\s+", "");
    return "Escuela de " + nombre;
  }

  /** Cargo de quien recibe la evaluación. */
  static String cargoDelEvaluado(Usuario docente) {
    return docente.getRol() == Rol.COORDINADOR
        ? "Docente y coordinador(a) de la " + nombreEscuela(docente.getFacultadDeAdscripcion())
        : "Docente";
  }

  /** Cargo con el que firma cada parte, como lo congeló el acta. */
  static String cargoDelFirmante(Acta acta, Rol papel) {
    DatosCongeladosActa datos = acta.getDatosCongelados();
    String congelado = papel == Rol.DOCENTE ? datos.docenteCargo() : datos.evaluadorCargo();
    return congelado != null ? congelado : cargoEnVivo(acta.getDocente(), papel);
  }

  /** Cargo de cada parte según los datos actuales del docente; se usa al congelar el acta. */
  public static String cargoEnVivo(Usuario docente, Rol papel) {
    return switch (papel) {
      case DOCENTE ->
          docente.getPrograma() == null
              ? cargoDelEvaluado(docente)
              : "Docente del programa " + docente.getPrograma().getNombre();
      case VICERRECTOR -> "Vicerrectoría Académica";
      default -> "Coordinación de la " + nombreEscuela(docente.getFacultadDeAdscripcion());
    };
  }

  /** El acta se expide cuando firma la última de las partes. */
  private static LocalDateTime fechaExpedicion(Acta acta) {
    return acta.getFirmas().stream()
        .map(FirmaActa::getFecha)
        .max(Comparator.naturalOrder())
        .orElseGet(LocalDateTime::now);
  }

  /**
   * Membrete y pie de cada página: logo y nombre de la universidad, cuadro de control (código,
   * página N de M) y, abajo, la dirección de verificación.
   */
  private static final class Membrete extends PdfPageEventHelper {
    private final byte[] logo;
    private final String codigo;
    private final String urlVerificacion;
    private final String formato;
    private final Font institucion = FontFactory.getFont(FontFactory.TIMES_BOLD, 12.5f, AZUL);
    private final Font dependencia = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TENUE);
    private final Font control = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, TENUE);
    private final Font controlFuerte = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, AZUL);
    private PdfTemplate totalPaginas;

    Membrete(byte[] logo, String codigo, String urlVerificacion, String formato) {
      this.logo = logo;
      this.codigo = codigo;
      this.urlVerificacion = urlVerificacion;
      this.formato = formato;
    }

    @Override
    public void onOpenDocument(PdfWriter escritor, Document documento) {
      totalPaginas = escritor.getDirectContent().createTemplate(20, 10);
    }

    @Override
    public void onEndPage(PdfWriter escritor, Document documento) {
      PdfContentByte lienzo = escritor.getDirectContent();
      float izquierda = documento.left();
      float derecha = documento.right();
      float arriba = documento.getPageSize().getHeight() - 38;
      float textoX = izquierda;
      if (logo != null) {
        try {
          Image imagen = Image.getInstance(logo);
          imagen.scaleToFit(118, 50);
          imagen.setAbsolutePosition(izquierda, arriba - 50);
          lienzo.addImage(imagen);
          textoX = izquierda + imagen.getScaledWidth() + 14;
          // Separador vertical entre el logo y el nombre.
          lienzo.setColorStroke(BORDE);
          lienzo.setLineWidth(0.8f);
          lienzo.moveTo(textoX - 7, arriba - 2);
          lienzo.lineTo(textoX - 7, arriba - 48);
          lienzo.stroke();
        } catch (IOException | DocumentException ilegible) {
          // Sin logo legible el membrete conserva el nombre de la universidad.
        }
      }
      escribir(lienzo, "CORPORACIÓN UNIVERSITARIA", institucion, textoX, arriba - 14, false);
      escribir(lienzo, "AUTÓNOMA DEL CAUCA", institucion, textoX, arriba - 28, false);
      escribir(
          lienzo,
          "Vicerrectoría Académica · Evaluación docente",
          dependencia,
          textoX,
          arriba - 42,
          false);

      if (formato != null) {
        escribir(lienzo, formato, control, derecha, arriba - 8, true);
      }
      escribir(lienzo, "Código  " + codigo, controlFuerte, derecha, arriba - 20, true);
      String pagina = "Página " + escritor.getPageNumber() + " de ";
      float anchoTotal = 4.5f;
      escribir(lienzo, pagina, control, derecha - anchoTotal - 2, arriba - 33, true);
      lienzo.addTemplate(totalPaginas, derecha - anchoTotal, arriba - 33);

      // Doble filete institucional bajo el membrete.
      lienzo.setColorStroke(AZUL);
      lienzo.setLineWidth(1.6f);
      lienzo.moveTo(izquierda, arriba - 60);
      lienzo.lineTo(derecha, arriba - 60);
      lienzo.stroke();
      lienzo.setColorStroke(DORADO);
      lienzo.setLineWidth(0.6f);
      lienzo.moveTo(izquierda, arriba - 63.5f);
      lienzo.lineTo(derecha, arriba - 63.5f);
      lienzo.stroke();

      float pie = documento.bottom() - 28;
      lienzo.setColorStroke(BORDE);
      lienzo.setLineWidth(0.6f);
      lienzo.moveTo(izquierda, pie + 14);
      lienzo.lineTo(derecha, pie + 14);
      lienzo.stroke();
      float centro = (izquierda + derecha) / 2;
      ColumnText.showTextAligned(
          lienzo,
          Element.ALIGN_CENTER,
          new Phrase(UNIVERSIDAD + " · " + CIUDAD + ", Cauca, Colombia", control),
          centro,
          pie,
          0);
      ColumnText.showTextAligned(
          lienzo,
          Element.ALIGN_CENTER,
          new Phrase(
              "Verifique la autenticidad de esta acta en " + urlVerificacion + codigo, control),
          centro,
          pie - 11,
          0);
    }

    @Override
    public void onCloseDocument(PdfWriter escritor, Document documento) {
      ColumnText.showTextAligned(
          totalPaginas,
          Element.ALIGN_LEFT,
          new Phrase(String.valueOf(escritor.getPageNumber() - 1), control),
          0,
          0,
          0);
    }

    private static void escribir(
        PdfContentByte lienzo, String texto, Font fuente, float x, float y, boolean aLaDerecha) {
      ColumnText.showTextAligned(
          lienzo,
          aLaDerecha ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT,
          new Phrase(texto, fuente),
          x,
          y,
          0);
    }
  }

  /** Quién evaluó al docente: la coordinación o, si es un coordinador, la vicerrectoría. */
  private static String evaluadorDe(Acta acta) {
    return acta.rolEvaluador() == Rol.VICERRECTOR ? "la vicerrectoría" : "la coordinación";
  }

  /**
   * Rótulo de la firma; la vicerrectoría firma en lugar de la coordinación al evaluar
   * coordinadores.
   */
  static String rotuloFirma(Rol rol) {
    return switch (rol) {
      case DOCENTE -> "Firma del docente";
      case VICERRECTOR -> "Firma vicerrectoría";
      default -> "Firma coordinación";
    };
  }

  /** Una columna por firma: rótulo, trazo sobre la línea y, debajo, nombre, cargo y fecha. */
  private PdfPTable firmas(Acta acta, Paragraph cierre, Font fuerte, Font rotulo, Font pequena)
      throws DocumentException {
    List<FirmaActa> ordenadas =
        acta.getFirmas().stream()
            .sorted(Comparator.comparing(f -> f.getRol() == Rol.DOCENTE ? 0 : 1))
            .toList();
    Font cargo = cargoFuente();
    PdfPTable tabla = new PdfPTable(Math.max(1, ordenadas.size()));
    tabla.setWidthPercentage(100);
    tabla.setSpacingBefore(10);
    PdfPCell constancia = new PdfPCell();
    constancia.addElement(cierre);
    constancia.setColspan(Math.max(1, ordenadas.size()));
    constancia.setBorder(Rectangle.NO_BORDER);
    constancia.setPadding(0);
    constancia.setPaddingBottom(22);
    tabla.addCell(constancia);
    for (FirmaActa firma : ordenadas) {
      PdfPTable bloque = new PdfPTable(1);
      PdfPCell titulo =
          new PdfPCell(new Phrase(rotuloFirma(firma.getRol()).toUpperCase(ESPANOL), rotulo));
      titulo.setBorder(Rectangle.NO_BORDER);
      titulo.setHorizontalAlignment(Element.ALIGN_CENTER);
      bloque.addCell(titulo);
      PdfPCell espacio;
      if (firma.getImagen() != null) {
        // El trazo debe caber en la celda (alto fijo 50) o OpenPDF no lo dibuja.
        espacio = new PdfPCell(imagen(firma.getImagen(), 150, 44), false);
      } else {
        espacio = new PdfPCell(new Phrase("Firmada electrónicamente", pequena));
      }
      espacio.setFixedHeight(50);
      espacio.setHorizontalAlignment(Element.ALIGN_CENTER);
      espacio.setVerticalAlignment(Element.ALIGN_BOTTOM);
      espacio.setBorder(Rectangle.BOTTOM);
      espacio.setBorderColor(Color.DARK_GRAY);
      espacio.setBorderWidth(0.8f);
      bloque.addCell(espacio);
      Paragraph pie = new Paragraph();
      pie.setLeading(14);
      pie.setAlignment(Element.ALIGN_CENTER);
      pie.add(new Chunk(firma.getNombre() + "\n", fuerte));
      pie.add(new Chunk(cargoDelFirmante(acta, firma.getRol()) + "\n", cargo));
      pie.add(new Chunk(firma.getCorreo() + "\n", pequena));
      // El hash del trazo queda en el registro de la firma y en el contenido verificable.
      pie.add(
          new Chunk(
              "Firmado el "
                  + firma.getFecha().format(FECHA_LARGA)
                  + " a las "
                  + firma.getFecha().format(HORA),
              pequena));
      // addElement respeta el interlineado del párrafo; el constructor usaría el de la celda.
      PdfPCell datos = new PdfPCell();
      datos.addElement(pie);
      datos.setBorder(Rectangle.NO_BORDER);
      datos.setHorizontalAlignment(Element.ALIGN_CENTER);
      datos.setPaddingTop(5);
      bloque.addCell(datos);
      PdfPCell contenedor = new PdfPCell(bloque);
      contenedor.setBorder(Rectangle.NO_BORDER);
      // Margen lateral: la línea de firma no ocupa toda la columna.
      contenedor.setPaddingLeft(16);
      contenedor.setPaddingRight(16);
      tabla.addCell(contenedor);
    }
    return tabla;
  }

  private static Paragraph subtitulo(int numero, String texto, Font fuente) {
    Paragraph parrafo = new Paragraph(numero + ".  " + texto.toUpperCase(ESPANOL), fuente);
    parrafo.setSpacingBefore(12);
    parrafo.setSpacingAfter(6);
    return parrafo;
  }

  private static Image imagen(byte[] datos, float ancho, float alto) throws DocumentException {
    try {
      Image imagen = Image.getInstance(datos);
      imagen.scaleToFit(ancho, alto);
      return imagen;
    } catch (IOException ilegible) {
      throw new DocumentException(ilegible);
    }
  }
}
