package com.uniautonoma.docentia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Aviso dentro de la app. Se inserta con SQL para que la deduplicación sea atómica. */
@Entity
@Table(name = "notificaciones")
public class Notificacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "usuario_id", nullable = false, updatable = false)
  private Long idUsuario;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40, updatable = false)
  private TipoNotificacion tipo;

  @Column(nullable = false, length = 150, updatable = false)
  private String titulo;

  @Column(nullable = false, length = 500, updatable = false)
  private String mensaje;

  @Column(length = 200, updatable = false)
  private String enlace;

  @Column(nullable = false)
  private boolean leida;

  @Column(name = "leida_en")
  private LocalDateTime leidaEn;

  @Column(name = "creada_en", nullable = false, updatable = false, insertable = false)
  private LocalDateTime creadaEn;

  protected Notificacion() {}

  public void marcarLeida(LocalDateTime ahora) {
    if (!leida) {
      leida = true;
      leidaEn = ahora;
    }
  }

  public Long getId() {
    return id;
  }

  public TipoNotificacion getTipo() {
    return tipo;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getMensaje() {
    return mensaje;
  }

  public String getEnlace() {
    return enlace;
  }

  public boolean isLeida() {
    return leida;
  }

  public LocalDateTime getLeidaEn() {
    return leidaEn;
  }

  public LocalDateTime getCreadaEn() {
    return creadaEn;
  }
}
