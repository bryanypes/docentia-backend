package com.uniautonoma.docentia.entity;

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

  public LocalDateTime getFechaCreacion() {
    return fechaCreacion;
  }
}
