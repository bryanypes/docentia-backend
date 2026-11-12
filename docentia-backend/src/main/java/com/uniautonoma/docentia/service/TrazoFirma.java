package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.exception.NoProcesableException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Trazo de una firma, dibujada en la página o subida como PNG. Solo acepta un PNG real (cabecera
 * comprobada), de hasta 300 KB y 2000 px por lado y con algo trazado: un lienzo en blanco no es una
 * firma. Lo que se guarda es una copia recodificada, sin metadatos; nunca el archivo recibido.
 */
public final class TrazoFirma {

  private static final String PREFIJO = "data:image/png;base64,";
  private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
  private static final int MAXIMO_BYTES = 300_000;
  private static final int MAXIMO_CARACTERES_BASE64 = ((MAXIMO_BYTES + 2) / 3) * 4;
  private static final int MAXIMO_LADO = 2000;
  private static final int MINIMO_PIXELES_TRAZADOS = 40;

  private TrazoFirma() {}

  /**
   * Decodifica y valida el trazo.
   *
   * @param dataUrl PNG como {@code data:image/png;base64,...}
   * @return los bytes del PNG recodificado, sin metadatos
   * @throws NoProcesableException si falta, no es un PNG válido o está en blanco
   */
  public static byte[] leer(String dataUrl) {
    if (dataUrl == null || dataUrl.isBlank()) {
      throw new NoProcesableException("Dibuja tu firma antes de firmar el acta");
    }
    if (!dataUrl.startsWith(PREFIJO)) {
      throw new NoProcesableException("La firma debe ser una imagen PNG");
    }
    if (dataUrl.length() - PREFIJO.length() > MAXIMO_CARACTERES_BASE64) {
      throw new NoProcesableException("La firma no es una imagen válida");
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
    try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(png))) {
      if (entrada == null) {
        throw new NoProcesableException("La firma no es una imagen válida");
      }
      Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
      if (!lectores.hasNext()) {
        throw new NoProcesableException("La firma no es una imagen válida");
      }
      ImageReader lector = lectores.next();
      try {
        lector.setInput(entrada, true, true);
        int ancho = lector.getWidth(0);
        int alto = lector.getHeight(0);
        if (ancho <= 0 || alto <= 0 || ancho > MAXIMO_LADO || alto > MAXIMO_LADO) {
          throw new NoProcesableException("La firma no es una imagen válida");
        }
        BufferedImage imagen = lector.read(0);
        if (imagen == null || pixelesTrazados(imagen) < MINIMO_PIXELES_TRAZADOS) {
          throw new NoProcesableException("Dibuja tu firma antes de firmar el acta");
        }
        // Se guarda una copia recodificada: solo píxeles, sin metadatos ni fragmentos extra.
        return recodificar(imagen);
      } finally {
        lector.dispose();
      }
    } catch (IOException ilegible) {
      throw new NoProcesableException("La firma no es una imagen válida");
    }
  }

  private static byte[] recodificar(BufferedImage original) throws IOException {
    BufferedImage limpia =
        new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D lienzo = limpia.createGraphics();
    try {
      lienzo.drawImage(original, 0, 0, null);
    } finally {
      lienzo.dispose();
    }
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    if (!ImageIO.write(limpia, "png", salida)) {
      throw new NoProcesableException("La firma no es una imagen válida");
    }
    return salida.toByteArray();
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
