package com.uniautonoma.docentia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** Comentario que el filtro excluyó de una solicitud de análisis, con la regla que lo excluyó. */
@Entity
@Table(name = "comentarios_filtrados")
public class ComentarioFiltrado {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idFiltrado;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_solicitud", nullable = false, updatable = false)
  private SolicitudAnalisis solicitud;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_comentario", nullable = false, updatable = false)
  private ComentarioEstudiante comentario;

  @Column(nullable = false, length = 40, updatable = false)
  private String categoria;

  @Column(nullable = false, length = 300, updatable = false)
  private String regla;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaFiltro;

  protected ComentarioFiltrado() {}

  public ComentarioFiltrado(
      SolicitudAnalisis solicitud,
      ComentarioEstudiante comentario,
      String categoria,
      String regla) {
    this.solicitud = solicitud;
    this.comentario = comentario;
    this.categoria = categoria;
    this.regla = regla.length() > 300 ? regla.substring(0, 300) : regla;
  }

  public ComentarioEstudiante getComentario() {
    return comentario;
  }

  public String getCategoria() {
    return categoria;
  }

  public String getRegla() {
    return regla;
  }

  public LocalDateTime getFechaFiltro() {
    return fechaFiltro;
  }
}
