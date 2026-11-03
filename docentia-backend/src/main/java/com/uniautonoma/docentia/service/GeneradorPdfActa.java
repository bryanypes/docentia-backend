package com.uniautonoma.docentia.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.CategoriaDesempeno;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.FirmaActa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoActa;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Arma el PDF de un acta firmada. Lleva el resultado final, nunca las notas por fuente (A4), porque
 * el docente lo descarga. El hash impreso es el del contenido; el del archivo se guarda aparte.
 */
@Component
public class GeneradorPdfActa {

  private static final DateTimeFormatter FORMATO_FECHA =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'hora de Colombia'");
  private static final Color AZUL = new Color(5, 54, 134);

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
    Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, AZUL);
    Font seccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, AZUL);
    Font texto = FontFactory.getFont(FontFactory.HELVETICA, 10);
    Font pequena = FontFactory.getFont(FontFactory.COURIER, 8);
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    Document documento = new Document(PageSize.LETTER, 50, 50, 50, 50);
    try {
      PdfWriter.getInstance(documento, salida);
      documento.open();
      String nombreActa =
          acta.getTipo() == TipoActa.COMPROMISO
              ? "Acta de compromiso docente"
              : "Acta de retroalimentación docente";
      documento.addTitle(nombreActa);
      documento.add(new Paragraph("DOCENTIA", titulo));
      documento.add(new Paragraph("Corporación Universitaria Autónoma del Cauca", texto));
      documento.add(new Paragraph(nombreActa, seccion));
      if (acta.getTipo() == TipoActa.RETROALIMENTACION) {
        documento.add(
            new Paragraph(
                "El docente recibió la retroalimentación de su evaluación en reunión con la"
                    + " coordinación.",
                texto));
      }
      documento.add(new Paragraph(" "));
      documento.add(new Paragraph("Docente: " + acta.getDocente().getNombreCompleto(), texto));
      documento.add(new Paragraph("Identificación: " + acta.getDocente().getCorreo(), texto));
      documento.add(new Paragraph("Periodo: " + acta.getPeriodo().getNombre(), texto));
      documento.add(new Paragraph("Versión del acta: " + acta.getVersion(), texto));
      if (acta.getMotivoCorreccion() != null) {
        documento.add(
            new Paragraph("Motivo de la corrección: " + acta.getMotivoCorreccion(), texto));
      }
      documento.add(new Paragraph(" "));
      documento.add(new Paragraph("Resultado", seccion));
      documento.add(new Paragraph("Nota final: " + acta.getPromedio(), texto));
      documento.add(new Paragraph("Categoría: " + acta.getCategoria().getEtiqueta(), texto));
      documento.add(
          new Paragraph(
              "Acción institucional: " + acta.getCategoria().getAccionInstitucional(), texto));
      if (!acta.getCompromisos().isEmpty()) {
        documento.add(new Paragraph(" "));
        documento.add(
            new Paragraph(
                acta.getCategoria() == CategoriaDesempeno.BAJO
                    ? "Plan de Mejoramiento Profesoral"
                    : "Compromisos",
                seccion));
        for (Compromiso compromiso : acta.getCompromisos()) {
          documento.add(
              new Paragraph(
                  "- "
                      + compromiso.getDescripcion()
                      + " (plazo: "
                      + compromiso.getFechaLimite()
                      + ")",
                  texto));
        }
      }
      documento.add(new Paragraph(" "));
      documento.add(new Paragraph("Firmas electrónicas", seccion));
      for (FirmaActa firma : acta.getFirmas()) {
        documento.add(
            new Paragraph(
                (firma.getRol() == Rol.DOCENTE ? "Docente: " : "Coordinación: ")
                    + firma.getNombre()
                    + " ("
                    + firma.getCorreo()
                    + ")",
                texto));
        documento.add(
            new Paragraph(
                "Fecha: " + firma.getFecha().format(FORMATO_FECHA) + " · IP: " + firma.getIp(),
                texto));
        if (firma.getImagen() != null) {
          documento.add(trazo(firma));
          documento.add(
              new Paragraph(
                  "Firma dibujada en DOCENTIA · SHA-256 " + firma.getHashImagen(), pequena));
        }
        documento.add(new Paragraph(" "));
      }
      documento.add(new Paragraph(" "));
      documento.add(new Paragraph("Sello de verificación", seccion));
      documento.add(new Paragraph("Código: " + codigo, texto));
      documento.add(new Paragraph("SHA-256 del contenido: " + hashContenido, pequena));
      documento.close();
    } catch (DocumentException excepcion) {
      throw new IllegalStateException("No se pudo generar el PDF del acta", excepcion);
    }
    return salida.toByteArray();
  }

  /** El trazo, a lo sumo 180 x 70 puntos, sobre la línea de la firma. */
  private Image trazo(FirmaActa firma) throws DocumentException {
    try {
      Image imagen = Image.getInstance(firma.getImagen());
      imagen.scaleToFit(180, 70);
      imagen.setSpacingBefore(4);
      return imagen;
    } catch (IOException ilegible) {
      throw new DocumentException(ilegible);
    }
  }
}
