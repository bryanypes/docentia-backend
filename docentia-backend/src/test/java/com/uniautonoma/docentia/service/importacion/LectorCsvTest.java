package com.uniautonoma.docentia.service.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LectorCsvTest {

  private final LectorCsv lector = new LectorCsv(3);

  @Test
  void leeUnCsvConComasEnUtf8() {
    TablaCsv tabla = lector.leer(utf8("correo,nota\nana@x.co,4.5\n"));

    assertThat(tabla.columnas()).containsExactly("correo", "nota");
    assertThat(tabla.delimitador()).isEqualTo(',');
    assertThat(tabla.codificacion()).isEqualTo(LectorCsv.UTF_8);
    assertThat(tabla.filas())
        .singleElement()
        .satisfies(
            fila -> {
              assertThat(fila.numero()).isEqualTo(2);
              assertThat(fila.valor("nota")).isEqualTo("4.5");
            });
  }

  @Test
  void quitaElBomYDetectaPuntoYComa() {
    byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    byte[] cuerpo = utf8("Correo;Planeación\nana@x.co;4,5\n");
    byte[] contenido = new byte[bom.length + cuerpo.length];
    System.arraycopy(bom, 0, contenido, 0, bom.length);
    System.arraycopy(cuerpo, 0, contenido, bom.length, cuerpo.length);

    TablaCsv tabla = lector.leer(contenido);

    assertThat(tabla.columnas()).containsExactly("Correo", "Planeación");
    assertThat(tabla.delimitador()).isEqualTo(';');
    assertThat(tabla.filas().get(0).valor("Planeación")).isEqualTo("4,5");
  }

  @Test
  void reconoceUnArchivoEnLatin1() {
    byte[] contenido =
        "Correo;Planeación\nana@x.co;Excelente metodología\n".getBytes(StandardCharsets.ISO_8859_1);

    TablaCsv tabla = lector.leer(contenido);

    assertThat(tabla.codificacion()).isEqualTo(LectorCsv.LATIN_1);
    assertThat(tabla.columnas()).contains("Planeación");
    assertThat(tabla.filas().get(0).valor("Planeación")).isEqualTo("Excelente metodología");
  }

  @Test
  void respetaComillasConSeparadoresYSaltosDeLineaYNumeraComoExcel() {
    String csv =
        "correo;comentario\n"
            + "ana@x.co;\"Dice; y también\nen dos líneas\"\n"
            + "\n"
            + ";;\n"
            + "luis@x.co;Bien\n";

    TablaCsv tabla = lector.leer(utf8(csv));

    assertThat(tabla.filas()).hasSize(2);
    assertThat(tabla.filas().get(0).valor("comentario"))
        .isEqualTo("Dice; y también\nen dos líneas");
    assertThat(tabla.filas().get(1).numero()).isEqualTo(4);
  }

  @Test
  void cuentaLosValoresQueSobranYCompletaLosQueFaltan() {
    TablaCsv tabla = lector.leer(utf8("a,b\n1,2,3,4\n5\n"));

    assertThat(tabla.filas().get(0).valoresDeMas()).isEqualTo(2);
    assertThat(tabla.filas().get(1).valor("b")).isEmpty();
  }

  @Test
  void unArchivoSoloConEncabezadoNoTieneFilas() {
    assertThat(lector.leer(utf8("a;b\n")).filas()).isEmpty();
  }

  @Test
  void rechazaArchivosQueNoSonCsvUtilizables() {
    assertThatThrownBy(() -> lector.leer(new byte[0])).hasMessage("El archivo está vacío");
    assertThatThrownBy(() -> lector.leer(utf8("\n\n  \n"))).hasMessage("El archivo está vacío");
    assertThatThrownBy(() -> lector.leer(utf8("solo una columna\nvalor\n")))
        .hasMessageContaining("coma o punto y coma");
    assertThatThrownBy(() -> lector.leer(utf8("Correo;correo\nx;y\n")))
        .hasMessageContaining("aparece más de una vez");
    assertThatThrownBy(() -> lector.leer(utf8("a;;c\n1;2;3\n")))
        .hasMessage("La columna 2 del encabezado no tiene nombre");
    assertThatThrownBy(() -> lector.leer(utf8("a;b\n1;\"sin cerrar\n")))
        .isInstanceOf(ArchivoCsvInvalidoException.class)
        .hasMessageContaining("comillas");
  }

  @Test
  void rechazaMasFilasDeLasPermitidas() {
    assertThatThrownBy(() -> lector.leer(utf8("a;b\n1;1\n2;2\n3;3\n4;4\n")))
        .hasMessage("El archivo supera el máximo de 3 filas de datos");
  }

  private byte[] utf8(String texto) {
    return texto.getBytes(StandardCharsets.UTF_8);
  }
}
