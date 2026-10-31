package com.uniautonoma.docentia.service.importacion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Lee el CSV que exporta Excel o Power BI. En español suelen venir con punto y coma y a veces en
 * Latin-1, así que detecta solos el BOM, la codificación y el delimitador.
 */
public class LectorCsv {

  public static final String UTF_8 = "UTF-8";
  public static final String LATIN_1 = "ISO-8859-1";

  private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
  // Windows-1252 es lo que realmente guarda Excel como "Latin-1": coincide con ISO-8859-1 en las
  // letras con tilde y además entiende comillas tipográficas y el símbolo del euro.
  private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

  private final int filasMaximas;

  public LectorCsv(int filasMaximas) {
    this.filasMaximas = filasMaximas;
  }

  /**
   * Lee el archivo completo.
   *
   * @param contenido bytes tal como llegaron
   * @return encabezados y filas
   * @throws ArchivoCsvInvalidoException si está vacío, no tiene delimitador reconocible, tiene
   *     comillas sin cerrar, encabezados repetidos o vacíos, o más filas de las permitidas
   */
  public TablaCsv leer(byte[] contenido) {
    if (contenido == null || contenido.length == 0) {
      throw new ArchivoCsvInvalidoException("El archivo está vacío");
    }
    Decodificado decodificado = decodificar(contenido);
    char delimitador = detectarDelimitador(decodificado.texto());
    CSVFormat formato =
        CSVFormat.DEFAULT
            .builder()
            .setDelimiter(delimitador)
            .setTrim(true)
            .setIgnoreSurroundingSpaces(true)
            .setIgnoreEmptyLines(true)
            .get();
    try (CSVParser parser = CSVParser.parse(decodificado.texto(), formato)) {
      Iterator<CSVRecord> registros = parser.iterator();
      if (!registros.hasNext()) {
        throw new ArchivoCsvInvalidoException("El archivo está vacío");
      }
      List<String> columnas = leerEncabezado(registros.next());
      List<TablaCsv.Fila> filas = new ArrayList<>();
      while (registros.hasNext()) {
        CSVRecord registro = registros.next();
        if (estaVacio(registro)) {
          continue;
        }
        if (filas.size() == filasMaximas) {
          throw new ArchivoCsvInvalidoException(
              "El archivo supera el máximo de " + filasMaximas + " filas de datos");
        }
        filas.add(leerFila(registro, columnas));
      }
      return new TablaCsv(columnas, filas, decodificado.codificacion(), delimitador);
    } catch (IOException | UncheckedIOException | IllegalStateException excepcion) {
      throw new ArchivoCsvInvalidoException(
          "El archivo no tiene un formato CSV válido; revise que las comillas estén cerradas");
    }
  }

  private Decodificado decodificar(byte[] contenido) {
    if (empiezaConBom(contenido)) {
      return new Decodificado(
          new String(
              contenido,
              BOM_UTF8.length,
              contenido.length - BOM_UTF8.length,
              StandardCharsets.UTF_8),
          UTF_8);
    }
    try {
      String texto =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(contenido))
              .toString();
      return new Decodificado(texto, UTF_8);
    } catch (CharacterCodingException noEsUtf8) {
      return new Decodificado(new String(contenido, WINDOWS_1252), LATIN_1);
    }
  }

  private boolean empiezaConBom(byte[] contenido) {
    return contenido.length >= BOM_UTF8.length
        && contenido[0] == BOM_UTF8[0]
        && contenido[1] == BOM_UTF8[1]
        && contenido[2] == BOM_UTF8[2];
  }

  /** Cuenta comas y puntos y comas fuera de comillas en la primera línea con texto. */
  private char detectarDelimitador(String texto) {
    String encabezado =
        texto
            .lines()
            .filter(linea -> !linea.isBlank())
            .findFirst()
            .orElseThrow(() -> new ArchivoCsvInvalidoException("El archivo está vacío"));
    int comas = 0;
    int puntosYComas = 0;
    boolean dentroDeComillas = false;
    for (char caracter : encabezado.toCharArray()) {
      if (caracter == '"') {
        dentroDeComillas = !dentroDeComillas;
      } else if (!dentroDeComillas && caracter == ',') {
        comas++;
      } else if (!dentroDeComillas && caracter == ';') {
        puntosYComas++;
      }
    }
    if (comas == 0 && puntosYComas == 0) {
      throw new ArchivoCsvInvalidoException(
          "No se reconoce el separador de columnas: el archivo debe usar coma o punto y coma");
    }
    return puntosYComas >= comas ? ';' : ',';
  }

  private List<String> leerEncabezado(CSVRecord registro) {
    List<String> columnas = new ArrayList<>();
    Set<String> normalizadas = new HashSet<>();
    for (int i = 0; i < registro.size(); i++) {
      String columna = registro.get(i).replace("﻿", "").trim();
      if (columna.isEmpty()) {
        throw new ArchivoCsvInvalidoException(
            "La columna " + (i + 1) + " del encabezado no tiene nombre");
      }
      if (!normalizadas.add(CampoImportacion.normalizar(columna))) {
        throw new ArchivoCsvInvalidoException(
            "La columna \"" + columna + "\" aparece más de una vez en el encabezado");
      }
      columnas.add(columna);
    }
    return List.copyOf(columnas);
  }

  private TablaCsv.Fila leerFila(CSVRecord registro, List<String> columnas) {
    Map<String, String> valores = new LinkedHashMap<>();
    for (int i = 0; i < columnas.size(); i++) {
      valores.put(columnas.get(i), i < registro.size() ? registro.get(i) : "");
    }
    int valoresDeMas = 0;
    for (int i = columnas.size(); i < registro.size(); i++) {
      if (!registro.get(i).isBlank()) {
        valoresDeMas++;
      }
    }
    // El número de registro del parser cuenta el encabezado como 1, igual que Excel.
    return new TablaCsv.Fila((int) registro.getRecordNumber(), valores, valoresDeMas);
  }

  private boolean estaVacio(CSVRecord registro) {
    for (String valor : registro) {
      if (!valor.isBlank()) {
        return false;
      }
    }
    return true;
  }

  private record Decodificado(String texto, String codificacion) {}
}
