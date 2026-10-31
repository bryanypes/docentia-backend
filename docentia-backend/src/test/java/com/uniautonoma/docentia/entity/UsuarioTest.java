package com.uniautonoma.docentia.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uniautonoma.docentia.exception.DatosInvalidosException;
import org.junit.jupiter.api.Test;

class UsuarioTest {

  private final Facultad facultad = new Facultad("Ingeniería");
  private final Programa programa = new Programa("Ingeniería de Software", facultad);

  @Test
  void cadaRolQuedaConElVinculoQueLeCorresponde() {
    assertThat(crear(Rol.COORDINADOR, facultad, null).getFacultad()).isSameAs(facultad);
    assertThat(crear(Rol.DOCENTE, null, programa).getPrograma()).isSameAs(programa);
    Usuario vicerrector = crear(Rol.VICERRECTOR, null, null);
    assertThat(vicerrector.getFacultad()).isNull();
    assertThat(vicerrector.getPrograma()).isNull();
  }

  @Test
  void elCoordinadorExigeFacultadYNoAceptaPrograma() {
    assertThatThrownBy(() -> crear(Rol.COORDINADOR, null, null))
        .isInstanceOf(DatosInvalidosException.class)
        .hasMessage("El coordinador debe estar vinculado a una facultad");
    assertThatThrownBy(() -> crear(Rol.COORDINADOR, facultad, programa))
        .isInstanceOf(DatosInvalidosException.class);
  }

  @Test
  void elDocenteExigeProgramaYNoAceptaFacultadDirecta() {
    assertThatThrownBy(() -> crear(Rol.DOCENTE, null, null))
        .isInstanceOf(DatosInvalidosException.class)
        .hasMessage("El docente debe estar vinculado a un programa");
    assertThatThrownBy(() -> crear(Rol.DOCENTE, facultad, programa))
        .isInstanceOf(DatosInvalidosException.class);
  }

  @Test
  void elVicerrectorNoSeVinculaANada() {
    assertThatThrownBy(() -> crear(Rol.VICERRECTOR, facultad, null))
        .isInstanceOf(DatosInvalidosException.class);
    assertThatThrownBy(() -> crear(Rol.VICERRECTOR, null, programa))
        .isInstanceOf(DatosInvalidosException.class);
  }

  @Test
  void cambiarDeRolReemplazaElVinculoAnterior() {
    Usuario usuario = crear(Rol.DOCENTE, null, programa);

    usuario.asignarAlcance(Rol.COORDINADOR, facultad, null);

    assertThat(usuario.getRol()).isEqualTo(Rol.COORDINADOR);
    assertThat(usuario.getPrograma()).isNull();
    assertThat(usuario.getFacultad()).isSameAs(facultad);
  }

  @Test
  void unCambioInvalidoNoDejaAlUsuarioAMedias() {
    Usuario usuario = crear(Rol.DOCENTE, null, programa);

    assertThatThrownBy(() -> usuario.asignarAlcance(Rol.COORDINADOR, null, null))
        .isInstanceOf(DatosInvalidosException.class);

    assertThat(usuario.getRol()).isEqualTo(Rol.DOCENTE);
    assertThat(usuario.getPrograma()).isSameAs(programa);
  }

  private Usuario crear(Rol rol, Facultad facultadUsuario, Programa programaUsuario) {
    return Usuario.crear(
        "Ana Ruiz", "ana@prueba.co", "hash", rol, facultadUsuario, programaUsuario);
  }
}
