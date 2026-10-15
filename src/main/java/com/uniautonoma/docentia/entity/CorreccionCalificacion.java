package com.uniautonoma.docentia.entity;

import com.uniautonoma.docentia.dto.EstadoPromedio;
import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Rastro de una calificación que cambió de notas después de registrada: la nota de la fuente y el
 * promedio del docente antes y después, quién la cambió y por qué. No se edita ni se borra.
 */
@Entity
@Table(name = "correcciones_calificacion")
public class CorreccionCalificacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idCorreccion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_calificacion", nullable = false, updatable = false)
  private Calificacion calificacion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_docente", nullable = false, updatable = false)
  private Usuario docente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private FuenteEvaluacion fuente;

  @Column(nullable = false, precision = 5, scale = 3, updatable = false)
  private BigDecimal notaAnterior;

  @Column(nullable = false, precision = 5, scale = 3, updatable = false)
  private BigDecimal notaNueva;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private EstadoPromedio estadoPromedioAnterior;

  @Column(precision = 3, scale = 2, updatable = false)
  private BigDecimal promedioAnterior;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private EstadoPromedio estadoPromedioNuevo;

  @Column(precision = 3, scale = 2, updatable = false)
  private BigDecimal promedioNuevo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_usuario", updatable = false)
  private Usuario usuario;

  @Column(length = 500, updatable = false)
  private String motivo;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaCorreccion;

  protected CorreccionCalificacion() {}

  public CorreccionCalificacion(
      Calificacion calificacion,
      BigDecimal notaAnterior,
      PromedioPonderadoResponse promedioAnterior,
      PromedioPonderadoResponse promedioNuevo,
      Usuario usuario,
      String motivo) {
    this.calificacion = calificacion;
    this.docente = calificacion.getDocente();
    this.periodo = calificacion.getPeriodo();
    this.fuente = calificacion.getFuente();
    this.notaAnterior = notaAnterior;
    this.notaNueva = calificacion.getNotas().promedio();
    this.estadoPromedioAnterior = promedioAnterior.estado();
    this.promedioAnterior = promedioAnterior.valor();
    this.estadoPromedioNuevo = promedioNuevo.estado();
    this.promedioNuevo = promedioNuevo.valor();
    this.usuario = usuario;
    this.motivo = motivo;
  }

  public Long getIdCorreccion() {
    return idCorreccion;
  }

  public PeriodoAcademico getPeriodo() {
    return periodo;
  }

  public FuenteEvaluacion getFuente() {
    return fuente;
  }

  public BigDecimal getNotaAnterior() {
    return notaAnterior;
  }

  public BigDecimal getNotaNueva() {
    return notaNueva;
  }

  public EstadoPromedio getEstadoPromedioAnterior() {
    return estadoPromedioAnterior;
  }

  public BigDecimal getPromedioAnterior() {
    return promedioAnterior;
  }

  public EstadoPromedio getEstadoPromedioNuevo() {
    return estadoPromedioNuevo;
  }

  public BigDecimal getPromedioNuevo() {
    return promedioNuevo;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public String getMotivo() {
    return motivo;
  }

  public LocalDateTime getFechaCorreccion() {
    return fechaCorreccion;
  }
}
