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

/**
 * Comentario abierto de un estudiante sobre un docente en un periodo. Solo lectura desde Java: se
 * inserta con ComentarioEstudianteRepository.insertarSiNoExiste para que recargar el mismo archivo
 * no lo duplique.
 */
@Entity
@Table(name = "comentarios_estudiantes")
public class ComentarioEstudiante {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idComentario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_docente", nullable = false, updatable = false)
  private Usuario docente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @Column(nullable = false, length = 2000, updatable = false)
  private String texto;

  @Column(nullable = false, length = 64, updatable = false)
  private String hashTexto;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_importacion", nullable = false, updatable = false)
  private Importacion importacion;

  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaRegistro;

  protected ComentarioEstudiante() {}

  public Long getIdComentario() {
    return idComentario;
  }

  public String getTexto() {
    return texto;
  }

  public Importacion getImportacion() {
    return importacion;
  }
}
