package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Programa;

public record ProgramaResponse(
    Long idPrograma, String nombre, Long idFacultad, String nombreFacultad) {

  public static ProgramaResponse desde(Programa programa) {
    return new ProgramaResponse(
        programa.getIdPrograma(),
        programa.getNombre(),
        programa.getFacultad().getIdFacultad(),
        programa.getFacultad().getNombre());
  }
}
