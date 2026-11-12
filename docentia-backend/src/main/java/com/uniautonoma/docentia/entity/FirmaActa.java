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

/** Firma de un acta: quién, con qué rol, cuándo y desde qué dirección. */
@Entity
@Table(name = "firmas_acta")
public class FirmaActa {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idFirma;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_acta", nullable = false, updatable = false)
  private Acta acta;

  @Column(name = "id_usuario", nullable = false, updatable = false)
  private Long idUsuario;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Rol rol;

  @Column(nullable = false, length = 150)
  private String nombre;

  @Column(nullable = false, length = 150)
  private String correo;

  @Column(nullable = false, length = 45)
  private String ip;

  @Column(nullable = false)
  private LocalDateTime fecha;

  protected FirmaActa() {}

  public FirmaActa(
      Acta acta,
      Long idUsuario,
      Rol rol,
      String nombre,
      String correo,
      String ip,
      LocalDateTime fecha) {
    this.acta = acta;
    this.idUsuario = idUsuario;
    this.rol = rol;
    this.nombre = nombre;
    this.correo = correo;
    this.ip = ip;
    this.fecha = fecha;
  }

  public Rol getRol() {
    return rol;
  }

  public String getNombre() {
    return nombre;
  }

  public String getCorreo() {
    return correo;
  }

  public String getIp() {
    return ip;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }
}
