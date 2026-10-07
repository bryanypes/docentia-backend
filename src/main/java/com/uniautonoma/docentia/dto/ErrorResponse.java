package com.uniautonoma.docentia.dto;

import java.util.Map;

public record ErrorResponse(String mensaje, Map<String, String> errores) {

  public ErrorResponse(String mensaje) {
    this(mensaje, Map.of());
  }
}
