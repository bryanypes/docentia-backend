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

/** Foto de un hallazgo después de cada cambio, con el modelo y la instrucción que lo generaron. */
@Entity
@Table(name = "versiones_hallazgo")
public class VersionHallazgo {

  public enum Accion {
    GENERADO,
    INCLUIDO,
    DESCARTADO,
    EDITADO
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idVersion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_hallazgo", nullable = false, updatable = false)
  private Hallazgo hallazgo;

  @Column(nullable = false)
  private int version;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Accion accion;

  @Column(nullable = false, length = 1000)
  private String texto;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EstadoHallazgo estado;

  @Column(length = 100)
  private String modelo;

  @Column(length = 40)
  private String versionInstruccion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_usuario")
  private Usuario usuario;

  @Column(length = 500)
  private String motivo;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fecha;

  protected VersionHallazgo() {}

  public VersionHallazgo(
      Hallazgo hallazgo, int version, Accion accion, Usuario usuario, String motivo) {
    this.hallazgo = hallazgo;
    this.version = version;
    this.accion = accion;
    this.texto = hallazgo.getTexto();
    this.estado = hallazgo.getEstado();
    this.modelo = hallazgo.getSolicitud().getModelo();
    this.versionInstruccion = hallazgo.getSolicitud().getVersionInstruccion();
    this.usuario = usuario;
    this.motivo = motivo;
  }

  public int getVersion() {
    return version;
  }

  public Accion getAccion() {
    return accion;
  }

  public String getTexto() {
    return texto;
  }

  public EstadoHallazgo getEstado() {
    return estado;
  }

  public String getModelo() {
    return modelo;
  }

  public String getVersionInstruccion() {
    return versionInstruccion;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public String getMotivo() {
    return motivo;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }
}
