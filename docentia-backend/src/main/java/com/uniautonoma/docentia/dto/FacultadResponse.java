package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Facultad;

public record FacultadResponse(Long idFacultad, String nombre) {

  public static FacultadResponse desde(Facultad facultad) {
    return new FacultadResponse(facultad.getIdFacultad(), facultad.getNombre());
  }
}
