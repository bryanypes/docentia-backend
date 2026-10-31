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

/** Fragmento textual de un comentario que respalda un hallazgo. */
@Entity
@Table(name = "citas_hallazgo")
public class CitaHallazgo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idCita;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_hallazgo", nullable = false, updatable = false)
  private Hallazgo hallazgo;

  /** Comentario que el modelo dice citar; puede no existir si el modelo se lo inventó. */
  private Long idComentarioCitado;

  @Column(nullable = false, length = 1000)
  private String fragmento;

  /** El fragmento aparece en un comentario no filtrado del docente y periodo. */
  @Column(nullable = false)
  private boolean verificada;

  protected CitaHallazgo() {}

  public CitaHallazgo(Long idComentarioCitado, String fragmento, boolean verificada) {
    this.idComentarioCitado = idComentarioCitado;
    this.fragmento = fragmento;
    this.verificada = verificada;
  }

  public boolean isVerificada() {
    return verificada;
  }

  void asignarHallazgo(Hallazgo hallazgo) {
    this.hallazgo = hallazgo;
  }

  public Long getIdCita() {
    return idCita;
  }

  public Long getIdComentarioCitado() {
    return idComentarioCitado;
  }

  public String getFragmento() {
    return fragmento;
  }
}
