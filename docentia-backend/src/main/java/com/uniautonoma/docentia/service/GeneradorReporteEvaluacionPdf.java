package com.uniautonoma.docentia.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GeneradorReporteEvaluacionPdf {

  public byte[] generar(
      PeriodoAcademico periodo, String alcance, List<FilaReporteEvaluacion> filas) {
    try {
      Document documento = new Document(Rectangle.A4, 36, 36, 36, 36);
      ByteArrayOutputStream salida = new ByteArrayOutputStream();
      PdfWriter.getInstance(documento, salida);
      documento.open();

      Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(5, 54, 134));
      Font normal = FontFactory.getFont(FontFactory.HELVETICA, 8);
      Font encabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

      Paragraph cabecera = new Paragraph("Reporte consolidado de evaluación docente", titulo);
      cabecera.setAlignment(Element.ALIGN_CENTER);
      documento.add(cabecera);
      documento.add(
          new Paragraph(
              "Periodo " + periodo.getNombre() + " · " + alcance,
              FontFactory.getFont(FontFactory.HELVETICA, 9)));
      documento.add(new Paragraph(" ", normal));

      PdfPTable tabla = new PdfPTable(new float[] {3.2f, 2.2f, 1.1f, 1.1f, 1.1f, 1.1f});
      tabla.setWidthPercentage(100);
      for (String texto :
          List.of(
              "Docente",
              "Programa / adscripción",
              "Estudiantes",
              "Institucional",
              "Autoevaluación",
              "Promedio")) {
        PdfPCell celda = new PdfPCell(new com.lowagie.text.Phrase(texto, encabezado));
        celda.setBackgroundColor(new Color(5, 54, 134));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(5);
        tabla.addCell(celda);
      }

      for (FilaReporteEvaluacion fila : filas) {
        tabla.addCell(celda(fila.docente(), normal));
        tabla.addCell(celda(fila.adscripcion(), normal));
        tabla.addCell(numero(fila.estudiantes(), normal));
        tabla.addCell(numero(fila.institucional(), normal));
        tabla.addCell(numero(fila.autoevaluacion(), normal));
        tabla.addCell(numero(fila.promedio(), normal));
      }

      documento.add(tabla);
      documento.add(
          new Paragraph(
              "El valor institucional corresponde a coordinación o vicerrectoría según el evaluado.",
              FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8)));
      documento.close();
      return salida.toByteArray();
    } catch (Exception error) {
      throw new IllegalStateException("No fue posible generar el reporte PDF", error);
    }
  }

  private PdfPCell celda(String texto, Font fuente) {
    PdfPCell celda = new PdfPCell(new com.lowagie.text.Phrase(texto == null ? "" : texto, fuente));
    celda.setPadding(4);
    return celda;
  }

  private PdfPCell numero(BigDecimal nota, Font fuente) {
    PdfPCell celda =
        new PdfPCell(
            new com.lowagie.text.Phrase(
                nota == null
                    ? "Pendiente"
                    : nota.setScale(2, java.math.RoundingMode.HALF_UP).toString(),
                fuente));
    celda.setHorizontalAlignment(Element.ALIGN_CENTER);
    celda.setPadding(4);
    return celda;
  }

  public record FilaReporteEvaluacion(
      String docente,
      String adscripcion,
      BigDecimal estudiantes,
      BigDecimal institucional,
      BigDecimal autoevaluacion,
      BigDecimal promedio) {}
}
