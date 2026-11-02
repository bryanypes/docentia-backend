package com.uniautonoma.docentia.service.campus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Forma del JSON de Campus y del seed {@code campus-clases-demo.json}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CampusSeed(List<CampusFacultad> facultades, List<CampusPersona> personas) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CampusFacultad(
      String codFacultad,
      String facultad,
      CampusCoordinador coordinador,
      List<CampusPrograma> programas) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CampusCoordinador(String nombre, String correo) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CampusPrograma(String codPrograma, String programa) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CampusPersona(
      String correo, String nombres, String apellidos, String programa, List<CampusRol> roles) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CampusRol(String rol, String codigo, List<CampusClase> clases) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CampusClase(
      String relacion,
      @JsonProperty("cod_curso") String codCurso,
      String curso,
      @JsonProperty("tipo_curso") String tipoCurso,
      String grupo,
      String jornada,
      List<CampusDocenteClase> docentes,
      @JsonProperty("num_estudiantes") Integer numEstudiantes) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CampusDocenteClase(
      String codigo, String correo, String nombres, String apellidos) {}
}
