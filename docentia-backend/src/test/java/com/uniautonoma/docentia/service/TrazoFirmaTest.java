package com.uniautonoma.docentia.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.uniautonoma.docentia.exception.NoProcesableException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class TrazoFirmaTest {

  private static final byte[] CABECERA_PNG = {
    (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
  };

  @Test
  void aceptaUnPngDeDimensionesValidasConTrazo() throws IOException {
    BufferedImage imagen = new BufferedImage(560, 190, BufferedImage.TYPE_INT_ARGB);
    for (int x = 20; x < 200; x++) {
      imagen.setRGB(x, 90, 0xff0b1f4d);
    }

    assertDoesNotThrow(() -> TrazoFirma.leer(dataUrl(imagen)));
  }

  @Test
  void rechazaDimensionesExcesivasAntesDeLeerLosPixeles() throws IOException {
    BufferedImage imagen = new BufferedImage(2001, 1, BufferedImage.TYPE_INT_ARGB);
    for (int x = 20; x < 180; x++) {
      imagen.setRGB(x, 0, 0xff0b1f4d);
    }

    assertThrows(NoProcesableException.class, () -> TrazoFirma.leer(dataUrl(imagen)));
  }

  @Test
  void rechazaUnDataUrlMasLargoDelLimite() {
    String dataUrl = "data:image/png;base64," + "A".repeat(400_004);

    assertThrows(NoProcesableException.class, () -> TrazoFirma.leer(dataUrl));
  }

  @Test
  void rechazaUnLienzoVacio() throws IOException {
    BufferedImage imagen = new BufferedImage(560, 190, BufferedImage.TYPE_INT_ARGB);

    assertThrows(NoProcesableException.class, () -> TrazoFirma.leer(dataUrl(imagen)));
  }

  @Test
  void rechazaUnSvgAunqueSeDisfraceDePng() {
    String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>";
    String b64 = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        NoProcesableException.class, () -> TrazoFirma.leer("data:image/svg+xml;base64," + b64));
    assertThrows(
        NoProcesableException.class, () -> TrazoFirma.leer("data:image/png;base64," + b64));
  }

  @Test
  void rechazaBytesQueSoloImitanLaCabeceraPng() {
    byte[] falso = new byte[64];
    System.arraycopy(CABECERA_PNG.clone(), 0, falso, 0, 8);
    String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(falso);

    assertThrows(NoProcesableException.class, () -> TrazoFirma.leer(dataUrl));
  }

  @Test
  void rechazaUnArchivoDeMasDe300Kb() {
    byte[] grande = new byte[300_001];
    System.arraycopy(CABECERA_PNG.clone(), 0, grande, 0, 8);
    String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(grande);

    assertThrows(NoProcesableException.class, () -> TrazoFirma.leer(dataUrl));
  }

  @Test
  void guardaUnaCopiaRecodificadaSinMetadatos() throws IOException {
    BufferedImage imagen = new BufferedImage(300, 100, BufferedImage.TYPE_INT_ARGB);
    for (int x = 20; x < 200; x++) {
      imagen.setRGB(x, 50, 0xff0b1f4d);
    }
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    ImageIO.write(imagen, "png", salida);
    byte[] conMetadatos = conTexto(salida.toByteArray(), "Author GPS 2.44,-76.6");
    String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(conMetadatos);

    byte[] guardado = TrazoFirma.leer(dataUrl);

    assertTrue(new String(conMetadatos, StandardCharsets.ISO_8859_1).contains("tEXt"));
    assertFalse(new String(guardado, StandardCharsets.ISO_8859_1).contains("tEXt"));
    assertFalse(new String(guardado, StandardCharsets.ISO_8859_1).contains("GPS"));
  }

  /** Inserta un fragmento tEXt justo después de la cabecera IHDR (8 + 25 bytes). */
  private static byte[] conTexto(byte[] png, String texto) {
    byte[] datos = texto.getBytes(StandardCharsets.ISO_8859_1);
    ByteBuffer fragmento = ByteBuffer.allocate(12 + datos.length);
    fragmento.putInt(datos.length);
    byte[] tipoYDatos = new byte[4 + datos.length];
    System.arraycopy("tEXt".getBytes(StandardCharsets.ISO_8859_1), 0, tipoYDatos, 0, 4);
    System.arraycopy(datos, 0, tipoYDatos, 4, datos.length);
    fragmento.put(tipoYDatos);
    CRC32 crc = new CRC32();
    crc.update(tipoYDatos);
    fragmento.putInt((int) crc.getValue());
    byte[] resultado = new byte[png.length + fragmento.capacity()];
    System.arraycopy(png, 0, resultado, 0, 33);
    System.arraycopy(fragmento.array(), 0, resultado, 33, fragmento.capacity());
    System.arraycopy(png, 33, resultado, 33 + fragmento.capacity(), png.length - 33);
    return resultado;
  }

  private String dataUrl(BufferedImage imagen) throws IOException {
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    ImageIO.write(imagen, "png", salida);
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
  }
}
