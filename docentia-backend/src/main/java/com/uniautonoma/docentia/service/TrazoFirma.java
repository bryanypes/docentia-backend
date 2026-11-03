package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.exception.NoProcesableException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * Trazo de una firma dibujada en la página. Solo acepta un PNG real, de un tamaño razonable y con
 * algo dibujado: un lienzo en blanco no es una firma.
 */
public final class TrazoFirma {

  private static final String PREFIJO = "data:image/png;base64,";
  private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
  private static final int MAXIMO_BYTES = 300_000;
  private static final int MAXIMO_LADO = 2000;
  private static final int MINIMO_PIXELES_TRAZADOS = 40;

  private TrazoFirma() {}

  /**
   * Decodifica y valida el trazo.
   *
   * @param dataUrl PNG como {@code data:image/png;base64,...}
   * @return los bytes del PNG
   * @throws NoProcesableException si falta, no es un PNG válido o está en blanco
   */
  public static byte[] leer(String dataUrl) {
    if (dataUrl == null || dataUrl.isBlank()) {
      throw new NoProcesableException("Dibuja tu firma antes de firmar el acta");
    }
    if (!dataUrl.startsWith(PREFIJO)) {
      throw new NoProcesableException("La firma debe ser una imagen PNG");
    }
    byte[] png;
    try {
      png = Base64.getDecoder().decode(dataUrl.substring(PREFIJO.length()));
    } catch (IllegalArgumentException invalida) {
      throw new NoProcesableException("La firma no es una imagen válida");
    }
    if (png.length > MAXIMO_BYTES
        || png.length < FIRMA_PNG.length
        || !Arrays.equals(Arrays.copyOf(png, FIRMA_PNG.length), FIRMA_PNG)) {
      throw new NoProcesableException("La firma no es una imagen válida");
    }
    BufferedImage imagen;
    try {
      imagen = ImageIO.read(new ByteArrayInputStream(png));
    } catch (IOException ilegible) {
      throw new NoProcesableException("La firma no es una imagen válida");
    }
    if (imagen == null
        || imagen.getWidth() > MAXIMO_LADO
        || imagen.getHeight() > MAXIMO_LADO
        || pixelesTrazados(imagen) < MINIMO_PIXELES_TRAZADOS) {
      throw new NoProcesableException("Dibuja tu firma antes de firmar el acta");
    }
    return png;
  }

  /** Píxeles opacos y oscuros: el trazo sobre el fondo transparente o blanco. */
  private static int pixelesTrazados(BufferedImage imagen) {
    int cuenta = 0;
    for (int y = 0; y < imagen.getHeight(); y += 2) {
      for (int x = 0; x < imagen.getWidth(); x += 2) {
        int argb = imagen.getRGB(x, y);
        int alfa = argb >>> 24;
        int brillo = ((argb >> 16) & 0xff) + ((argb >> 8) & 0xff) + (argb & 0xff);
        if (alfa > 128 && brillo < 600) {
          cuenta++;
        }
      }
    }
    return cuenta;
  }
}
