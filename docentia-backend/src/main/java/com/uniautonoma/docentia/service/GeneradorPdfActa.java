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
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.uniautonoma.docentia.config.NotificacionProperties;
import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.CategoriaDesempeno;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.FirmaActa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoActa;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
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

  private static final DateTimeFormatter FORMATO_FECHA =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'hora de Colombia'");
  private static final Color AZUL = new Color(5, 54, 134);
  private static final Color AZUL_SUAVE = new Color(232, 237, 248);
  private static final Color BORDE = new Color(214, 206, 188);
  private static final Color TENUE = new Color(102, 98, 90);

  private final byte[] logo;
  private final String urlVerificacion;

  public GeneradorPdfActa(
      ResourceLoader recursos,
      @Value("${docentia.actas.logo:classpath:marca/logo-uniautonoma.png}") String rutaLogo,
      NotificacionProperties notificaciones) {
    this.logo = leerLogo(recursos.getResource(rutaLogo));
    this.urlVerificacion = notificaciones.urlAplicacion() + "/verificar-acta/";
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
        firmas);
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
    Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, AZUL);
    Font seccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11.5f, AZUL);
    Font texto = FontFactory.getFont(FontFactory.HELVETICA, 10);
    Font fuerte = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    Font rotulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TENUE);
    Font pequena = FontFactory.getFont(FontFactory.COURIER, 7.5f, TENUE);
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    Document documento = new Document(PageSize.LETTER, 48, 48, 44, 44);
    try {
      PdfWriter.getInstance(documento, salida);
      documento.open();
      String nombreActa =
          acta.getTipo() == TipoActa.COMPROMISO
              ? "Acta de compromiso docente"
              : "Acta de retroalimentación docente";
      documento.addTitle(nombreActa);
      documento.add(encabezado(nombreActa, titulo, texto));
      if (acta.getTipo() == TipoActa.RETROALIMENTACION) {
        documento.add(
            new Paragraph(
                "El docente recibió la retroalimentación de su evaluación en reunión con la"
                    + " coordinación.",
                texto));
      }

      documento.add(subtitulo("Datos del docente", seccion));
      PdfPTable datos = tabla(new float[] {1, 1, 1, 1});
      dato(datos, "DOCENTE", acta.getDocente().getNombreCompleto(), rotulo, fuerte);
      dato(datos, "IDENTIFICACIÓN", acta.getDocente().getCorreo(), rotulo, texto);
      dato(datos, "PERIODO", acta.getPeriodo().getNombre(), rotulo, texto);
      dato(datos, "VERSIÓN DEL ACTA", String.valueOf(acta.getVersion()), rotulo, texto);
      documento.add(datos);
      if (acta.getMotivoCorreccion() != null) {
        documento.add(
            new Paragraph("Motivo de la corrección: " + acta.getMotivoCorreccion(), texto));
      }

      documento.add(subtitulo("Resultado", seccion));
      PdfPTable resultado = tabla(new float[] {1, 1.4f, 2.6f});
      dato(resultado, "NOTA FINAL", String.valueOf(acta.getPromedio()), rotulo, titulo);
      dato(resultado, "CATEGORÍA", acta.getCategoria().getEtiqueta(), rotulo, fuerte);
      dato(
          resultado,
          "ACCIÓN INSTITUCIONAL",
          acta.getCategoria().getAccionInstitucional(),
          rotulo,
          texto);
      documento.add(resultado);

      if (!acta.getCompromisos().isEmpty()) {
        documento.add(
            subtitulo(
                acta.getCategoria() == CategoriaDesempeno.BAJO
                    ? "Plan de Mejoramiento Profesoral"
                    : "Compromisos",
                seccion));
        PdfPTable compromisos = tabla(new float[] {0.4f, 4, 1.3f});
        encabezadoTabla(compromisos, rotulo, "#", "COMPROMISO", "PLAZO");
        int numero = 1;
        for (Compromiso compromiso : acta.getCompromisos()) {
          celda(compromisos, String.valueOf(numero++), texto);
          celda(compromisos, compromiso.getDescripcion(), texto);
          celda(compromisos, String.valueOf(compromiso.getFechaLimite()), texto);
        }
        documento.add(compromisos);
      }

      documento.add(subtitulo("Firmas electrónicas", seccion));
      documento.add(firmas(acta, texto, fuerte, rotulo, pequena));

      documento.add(subtitulo("Constancia de firmas y verificación", seccion));
      documento.add(certificado(acta, codigo, hashContenido, texto, fuerte, pequena));
      documento.close();
    } catch (DocumentException excepcion) {
      throw new IllegalStateException("No se pudo generar el PDF del acta", excepcion);
    }
    return salida.toByteArray();
  }

  /** Logo (si la institución lo configuró), nombre del sistema y tipo de acta. */
  private PdfPTable encabezado(String nombreActa, Font titulo, Font texto)
      throws DocumentException {
    PdfPTable cabecera = new PdfPTable(logo == null ? new float[] {1} : new float[] {1.3f, 4});
    cabecera.setWidthPercentage(100);
    if (logo != null) {
      PdfPCell celdaLogo = new PdfPCell(imagen(logo, 120, 60), false);
      celdaLogo.setBorder(Rectangle.NO_BORDER);
      celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
      cabecera.addCell(celdaLogo);
    }
    Paragraph textos = new Paragraph();
    textos.add(new Chunk("DOCENTIA\n", titulo));
    textos.add(new Chunk("Corporación Universitaria Autónoma del Cauca\n", texto));
    textos.add(new Chunk(nombreActa, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12.5f, AZUL)));
    PdfPCell celdaTexto = new PdfPCell(textos);
    celdaTexto.setBorder(Rectangle.BOTTOM);
    celdaTexto.setBorderColor(AZUL);
    celdaTexto.setBorderWidth(1.5f);
    celdaTexto.setPaddingBottom(8);
    celdaTexto.setVerticalAlignment(Element.ALIGN_MIDDLE);
    cabecera.addCell(celdaTexto);
    if (logo != null) {
      cabecera.getRow(0).getCells()[0].setBorder(Rectangle.BOTTOM);
      cabecera.getRow(0).getCells()[0].setBorderColor(AZUL);
      cabecera.getRow(0).getCells()[0].setBorderWidth(1.5f);
    }
    cabecera.setSpacingAfter(6);
    return cabecera;
  }

  /** Una columna por firma: el trazo sobre la línea, y debajo quién, con qué rol y cuándo. */
  private PdfPTable firmas(Acta acta, Font texto, Font fuerte, Font rotulo, Font pequena)
      throws DocumentException {
    List<FirmaActa> ordenadas =
        acta.getFirmas().stream()
            .sorted(Comparator.comparing(f -> f.getRol() == Rol.DOCENTE ? 0 : 1))
            .toList();
    PdfPTable tabla = new PdfPTable(Math.max(1, ordenadas.size()));
    tabla.setWidthPercentage(100);
    for (FirmaActa firma : ordenadas) {
      PdfPTable bloque = new PdfPTable(1);
      PdfPCell espacio;
      if (firma.getImagen() != null) {
        espacio = new PdfPCell(imagen(firma.getImagen(), 190, 70), false);
      } else {
        espacio = new PdfPCell(new Phrase("Firmada electrónicamente", texto));
      }
      espacio.setFixedHeight(78);
      espacio.setHorizontalAlignment(Element.ALIGN_CENTER);
      espacio.setVerticalAlignment(Element.ALIGN_BOTTOM);
      espacio.setBorder(Rectangle.BOTTOM);
      espacio.setBorderColor(TENUE);
      bloque.addCell(espacio);
      Paragraph pie = new Paragraph();
      pie.add(new Chunk(firma.getRol() == Rol.DOCENTE ? "Docente: " : "Coordinación: ", rotulo));
      pie.add(new Chunk(firma.getNombre() + "\n", fuerte));
      pie.add(new Chunk(firma.getCorreo() + "\n", texto));
      pie.add(new Chunk("Fecha: " + firma.getFecha().format(FORMATO_FECHA) + "\n", texto));
      if (firma.getHashImagen() != null) {
        pie.add(
            new Chunk("\nFirma registrada en DOCENTIA · SHA-256 " + firma.getHashImagen(), pequena));
      }
      PdfPCell datos = new PdfPCell(pie);
      datos.setBorder(Rectangle.NO_BORDER);
      datos.setPaddingTop(6);
      bloque.addCell(datos);
      PdfPCell contenedor = new PdfPCell(bloque);
      contenedor.setBorder(Rectangle.NO_BORDER);
      contenedor.setPadding(6);
      tabla.addCell(contenedor);
    }
    return tabla;
  }

  /** Recuadro con el registro de firmas y los datos para verificar la integridad del acta. */
  private PdfPTable certificado(
      Acta acta, String codigo, String hashContenido, Font texto, Font fuerte, Font pequena)
      throws DocumentException {
    PdfPTable caja = new PdfPTable(logo == null ? new float[] {1} : new float[] {1, 5});
    caja.setWidthPercentage(100);
    if (logo != null) {
      PdfPCell celdaLogo = new PdfPCell(imagen(logo, 70, 40), false);
      celdaLogo.setBorder(Rectangle.NO_BORDER);
      celdaLogo.setBackgroundColor(AZUL_SUAVE);
      celdaLogo.setPadding(10);
      celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
      caja.addCell(celdaLogo);
    }
    Paragraph cuerpo = new Paragraph();
    cuerpo.setLeading(13);
    cuerpo.add(
        new Chunk(
            "DOCENTIA registra que "
                + acta.getDocente().getNombreCompleto()
                + " ("
                + acta.getDocente().getCorreo()
                + ") y la coordinación firmaron esta acta en la plataforma, después de la reunión de"
                + " retroalimentación del periodo "
                + acta.getPeriodo().getNombre()
                + ". DOCENTIA conserva el trazo, la identidad de la cuenta, la fecha y el hash de"
                + " cada firma. Este registro no incluye un certificado digital criptográfico; las"
                + " versiones firmadas se conservan y cualquier corrección crea una nueva versión.\n",
            texto));
    cuerpo.add(new Chunk("Código de verificación: ", fuerte));
    cuerpo.add(new Chunk(codigo + "\n", fuerte));
    cuerpo.add(new Chunk("Verifícala en " + urlVerificacion + codigo + "\n", texto));
    cuerpo.add(new Chunk("SHA-256 del contenido: " + hashContenido, pequena));
    PdfPCell celda = new PdfPCell(cuerpo);
    celda.setBorder(Rectangle.NO_BORDER);
    celda.setBackgroundColor(AZUL_SUAVE);
    celda.setPadding(10);
    caja.addCell(celda);
    return caja;
  }

  private static Paragraph subtitulo(String texto, Font fuente) {
    Paragraph parrafo = new Paragraph(texto, fuente);
    parrafo.setSpacingBefore(12);
    parrafo.setSpacingAfter(5);
    return parrafo;
  }

  private static PdfPTable tabla(float[] anchos) throws DocumentException {
    PdfPTable tabla = new PdfPTable(anchos);
    tabla.setWidthPercentage(100);
    return tabla;
  }

  private static void dato(
      PdfPTable tabla, String rotulo, String valor, Font fRotulo, Font fValor) {
    Paragraph parrafo = new Paragraph();
    parrafo.add(new Chunk(rotulo + "\n", fRotulo));
    parrafo.add(new Chunk(valor, fValor));
    PdfPCell celda = new PdfPCell(parrafo);
    celda.setBorderColor(BORDE);
    celda.setPadding(7);
    tabla.addCell(celda);
  }

  private static void encabezadoTabla(PdfPTable tabla, Font fuente, String... titulos) {
    for (String titulo : titulos) {
      PdfPCell celda = new PdfPCell(new Phrase(titulo, fuente));
      celda.setBackgroundColor(AZUL_SUAVE);
      celda.setBorderColor(BORDE);
      celda.setPadding(6);
      tabla.addCell(celda);
    }
  }

  private static void celda(PdfPTable tabla, String texto, Font fuente) {
    PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
    celda.setBorderColor(BORDE);
    celda.setPadding(6);
    tabla.addCell(celda);
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
