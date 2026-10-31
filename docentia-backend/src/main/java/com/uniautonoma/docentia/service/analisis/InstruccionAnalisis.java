package com.uniautonoma.docentia.service.analisis;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

/**
 * Texto de instrucción que se envía al modelo. Vive versionado en resources/prompts: cambiarlo es
 * crear un archivo nuevo (analisis-v2.txt) y subir la versión, así cada borrador dice con qué
 * instrucción se generó.
 */
public final class InstruccionAnalisis {

  public static final String VERSION = "analisis-v1";

  private final String texto;

  private InstruccionAnalisis(String texto) {
    this.texto = texto;
  }

  public static InstruccionAnalisis cargar() {
    try (InputStream entrada =
        new ClassPathResource("prompts/" + VERSION + ".txt").getInputStream()) {
      return new InstruccionAnalisis(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException excepcion) {
      throw new UncheckedIOException("No se encontró la instrucción " + VERSION, excepcion);
    }
  }

  public String texto() {
    return texto;
  }

  public String version() {
    return VERSION;
  }
}
