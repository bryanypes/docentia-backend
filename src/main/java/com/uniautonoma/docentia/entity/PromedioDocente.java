package com.uniautonoma.docentia.entity;

import com.uniautonoma.docentia.dto.EstadoPromedio;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.List;

/**
 * Promedio ponderado de un docente en un periodo, tal como quedó la última vez que cambió una de
 * sus calificaciones o las ponderaciones del periodo. Hay uno solo por docente y periodo.
 */
@Entity
@Table(name = "promedios_docente")
public class PromedioDocente {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idPromedio;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_docente", nullable = false, updatable = false)
  private Usuario docente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @Column(precision = 3, scale = 2)
  private BigDecimal valor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoPromedio estado;

  @Convert(converter = FuentesPendientesConverter.class)
  @Column(length = 100)
  private List<FuenteEvaluacion> fuentesPendientes;

  @Column(nullable = false)
  private LocalDateTime fechaCalculo;

  // Solo lectura desde Java: las filas las escribe PromedioDocenteRepository.guardar con un upsert.
  protected PromedioDocente() {}

  public Long getIdPromedio() {
    return idPromedio;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public EstadoPromedio getEstado() {
    return estado;
  }

  public List<FuenteEvaluacion> getFuentesPendientes() {
    return fuentesPendientes;
  }

  public LocalDateTime getFechaCalculo() {
    return fechaCalculo;
  }
}
