package com.uniautonoma.docentia.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.uniautonoma.docentia.exception.NoProcesableException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class TrazoFirmaTest {

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

  private String dataUrl(BufferedImage imagen) throws IOException {
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    ImageIO.write(imagen, "png", salida);
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
  }
}
