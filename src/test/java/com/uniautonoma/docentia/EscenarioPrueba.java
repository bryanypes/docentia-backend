package com.uniautonoma.docentia;

import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import org.springframework.stereotype.Component;

/** Crea los datos que se repiten entre pruebas: facultades, programas y usuarios por rol. */
@Component
public class EscenarioPrueba {

  private static final String HASH = "hash-de-prueba";

  private final FacultadRepository facultadRepository;
  private final ProgramaRepository programaRepository;
  private final UsuarioRepository usuarioRepository;

  public EscenarioPrueba(
      FacultadRepository facultadRepository,
      ProgramaRepository programaRepository,
      UsuarioRepository usuarioRepository) {
    this.facultadRepository = facultadRepository;
    this.programaRepository = programaRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public Facultad crearFacultad(String nombre) {
    return facultadRepository.save(new Facultad(nombre));
  }

  public Programa crearPrograma(String nombre, Facultad facultad) {
    return programaRepository.save(new Programa(nombre, facultad));
  }

  public Usuario crearDocente(String nombre, Programa programa) {
    return usuarioRepository.save(Usuario.crearDocente(nombre, correoDe(nombre), HASH, programa));
  }

  public Usuario crearCoordinador(String nombre, Facultad facultad) {
    return usuarioRepository.save(
        Usuario.crearCoordinador(nombre, correoDe(nombre), HASH, facultad));
  }

  public Usuario crearVicerrector(String nombre) {
    return usuarioRepository.save(Usuario.crearVicerrector(nombre, correoDe(nombre), HASH));
  }

  private String correoDe(String nombre) {
    return nombre.toLowerCase().replace(' ', '.') + "@uniautonoma.edu.co";
  }
}
