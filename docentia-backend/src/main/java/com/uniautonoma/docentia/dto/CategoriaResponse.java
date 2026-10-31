package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.CategoriaDesempeno;

/** Categoría de desempeño con su etiqueta y la acción institucional, que es informativa. */
public record CategoriaResponse(
    CategoriaDesempeno codigo, String etiqueta, String accionInstitucional) {

  public static CategoriaResponse desde(CategoriaDesempeno categoria) {
    return categoria == null
        ? null
        : new CategoriaResponse(
            categoria, categoria.getEtiqueta(), categoria.getAccionInstitucional());
  }
}
