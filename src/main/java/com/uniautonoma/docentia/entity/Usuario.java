package com.uniautonoma.docentia.entity;

import com.uniautonoma.docentia.exception.DatosInvalidosException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Persona que ingresa al sistema. El rol determina el alcance: el coordinador queda ligado a una
 * facultad, el docente a un programa y el vicerrector a ninguno de los dos.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idUsuario;

  @Column(nullable = false, length = 150)
  private String nombreCompleto;

  @Column(nullable = false, length = 150)
  private String correo;

  @Column(nullable = false, length = 100)
  private String contrasenaHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Rol rol;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_facultad")
  private Facultad facultad;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_programa")
  private Programa programa;

  @Column(nullable = false)
  private boolean esActivo = true;

  /** La contraseña la asignó la vicerrectoría y el usuario todavía no la ha cambiado. */
  @Column(nullable = false)
  private boolean debeCambiarContrasena;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  protected Usuario() {}

  private Usuario(String nombreCompleto, String correo, String contrasenaHash, Rol rol) {
    this.nombreCompleto = nombreCompleto;
    this.correo = correo;
    this.contrasenaHash = contrasenaHash;
    this.rol = rol;
  }

  public static Usuario crearCoordinador(
      String nombreCompleto, String correo, String contrasenaHash, Facultad facultad) {
    Usuario coordinador = new Usuario(nombreCompleto, correo, contrasenaHash, Rol.COORDINADOR);
    coordinador.facultad = facultad;
    return coordinador;
  }

  public static Usuario crearDocente(
      String nombreCompleto, String correo, String contrasenaHash, Programa programa) {
    Usuario docente = new Usuario(nombreCompleto, correo, contrasenaHash, Rol.DOCENTE);
    docente.programa = programa;
    return docente;
  }

  public static Usuario crearVicerrector(
      String nombreCompleto, String correo, String contrasenaHash) {
    return new Usuario(nombreCompleto, correo, contrasenaHash, Rol.VICERRECTOR);
  }

  /**
   * Crea un usuario con cualquiera de los tres roles, validando su vínculo.
   *
   * @throws DatosInvalidosException si el vínculo no corresponde al rol
   */
  public static Usuario crear(
      String nombreCompleto,
      String correo,
      String contrasenaHash,
      Rol rol,
      Facultad facultad,
      Programa programa) {
    Usuario usuario = new Usuario(nombreCompleto, correo, contrasenaHash, rol);
    usuario.asignarAlcance(rol, facultad, programa);
    return usuario;
  }

  /**
   * Cambia el rol y el vínculo del usuario. Es la misma regla que la base de datos exige con
   * ck_usuarios_alcance, pero aquí produce un mensaje que el usuario entiende.
   *
   * @param rol el único rol que tendrá el usuario
   * @param facultad facultad del coordinador; nula para los demás roles
   * @param programa programa del docente; nulo para los demás roles
   * @throws DatosInvalidosException si el vínculo no corresponde al rol
   */
  public void asignarAlcance(Rol rol, Facultad facultad, Programa programa) {
    if (rol == null) {
      throw new DatosInvalidosException("El rol es obligatorio");
    }
    switch (rol) {
      case COORDINADOR -> {
        if (facultad == null) {
          throw new DatosInvalidosException("El coordinador debe estar vinculado a una facultad");
        }
        if (programa != null) {
          throw new DatosInvalidosException(
              "El coordinador se vincula a una facultad, no a un programa");
        }
      }
      case DOCENTE -> {
        if (programa == null) {
          throw new DatosInvalidosException("El docente debe estar vinculado a un programa");
        }
        if (facultad != null) {
          throw new DatosInvalidosException(
              "El docente se vincula a un programa; su facultad es la del programa");
        }
      }
      case VICERRECTOR -> {
        if (facultad != null || programa != null) {
          throw new DatosInvalidosException(
              "El vicerrector no se vincula a una facultad ni a un programa");
        }
      }
    }
    this.rol = rol;
    this.facultad = facultad;
    this.programa = programa;
  }

  public Long getIdUsuario() {
    return idUsuario;
  }

  public String getNombreCompleto() {
    return nombreCompleto;
  }

  public void setNombreCompleto(String nombreCompleto) {
    this.nombreCompleto = nombreCompleto;
  }

  public String getCorreo() {
    return correo;
  }

  public void setCorreo(String correo) {
    this.correo = correo;
  }

  public String getContrasenaHash() {
    return contrasenaHash;
  }

  public void setContrasenaHash(String contrasenaHash) {
    this.contrasenaHash = contrasenaHash;
  }

  public Rol getRol() {
    return rol;
  }

  public Facultad getFacultad() {
    return facultad;
  }

  public Programa getPrograma() {
    return programa;
  }

  public boolean getEsActivo() {
    return esActivo;
  }

  public void setEsActivo(boolean esActivo) {
    this.esActivo = esActivo;
  }

  public boolean getDebeCambiarContrasena() {
    return debeCambiarContrasena;
  }

  /** Contraseña puesta por la vicerrectoría: el usuario debe cambiarla antes de usar el sistema. */
  public void asignarContrasenaTemporal(String contrasenaHash) {
    this.contrasenaHash = contrasenaHash;
    this.debeCambiarContrasena = true;
  }

  /** Contraseña elegida por el propio usuario. */
  public void cambiarContrasenaPropia(String contrasenaHash) {
    this.contrasenaHash = contrasenaHash;
    this.debeCambiarContrasena = false;
  }

  public LocalDateTime getFechaCreacion() {
    return fechaCreacion;
  }
}
