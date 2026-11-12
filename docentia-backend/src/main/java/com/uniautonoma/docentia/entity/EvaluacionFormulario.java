package com.uniautonoma.docentia.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Formulario de un docente en un periodo. Al enviarse queda fijo. */
@Entity
@Table(name = "evaluaciones_formulario")
public class EvaluacionFormulario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "periodo_id", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "docente_id", nullable = false, updatable = false)
  private Usuario docente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "evaluador_usuario_id", nullable = false, updatable = false)
  private Usuario evaluador;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private TipoFormulario tipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private EstadoFormulario estado = EstadoFormulario.BORRADOR;

  @Column(name = "instrumento_version", nullable = false, length = 40, updatable = false)
  private String instrumentoVersion;

  @Column(precision = 6, scale = 4)
  private BigDecimal valor;

  @Column(name = "enviada_en")
  private LocalDateTime enviadaEn;

  @Version private long version;

  /** Valor de cada descriptor respondido, por id del descriptor. */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "respuestas_descriptor",
      joinColumns = @JoinColumn(name = "evaluacion_id"))
  @MapKeyColumn(name = "descriptor_id")
  @Column(name = "valor", nullable = false)
  private Map<Long, Short> respuestas = new HashMap<>();

  /** Comentario opcional de cada criterio, por id del criterio. */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "comentarios_criterio", joinColumns = @JoinColumn(name = "evaluacion_id"))
  @MapKeyColumn(name = "criterio_id")
  @Column(name = "texto", nullable = false, length = 1000)
  private Map<Long, String> comentarios = new HashMap<>();

  protected EvaluacionFormulario() {}

  public EvaluacionFormulario(
      PeriodoAcademico periodo,
      Usuario docente,
      Usuario evaluador,
      TipoFormulario tipo,
      String instrumentoVersion) {
    this.periodo = periodo;
    this.docente = docente;
    this.evaluador = evaluador;
    this.tipo = tipo;
    this.instrumentoVersion = instrumentoVersion;
  }

  public void reemplazar(Map<Long, Short> nuevasRespuestas, Map<Long, String> nuevosComentarios) {
    respuestas.clear();
    respuestas.putAll(nuevasRespuestas);
    comentarios.clear();
    comentarios.putAll(nuevosComentarios);
  }

  public void enviar(BigDecimal valorFuente, LocalDateTime ahora) {
    estado = EstadoFormulario.ENVIADA;
    valor = valorFuente;
    enviadaEn = ahora;
  }

  public boolean estaEnviada() {
    return estado == EstadoFormulario.ENVIADA;
  }

  public Long getId() {
    return id;
  }

  public PeriodoAcademico getPeriodo() {
    return periodo;
  }

  public Usuario getDocente() {
    return docente;
  }

  public Usuario getEvaluador() {
    return evaluador;
  }

  public TipoFormulario getTipo() {
    return tipo;
  }

  public EstadoFormulario getEstado() {
    return estado;
  }

  public String getInstrumentoVersion() {
    return instrumentoVersion;
  }

  public LocalDateTime getEnviadaEn() {
    return enviadaEn;
  }

  public Map<Long, Short> getRespuestas() {
    return respuestas;
  }

  public Map<Long, String> getComentarios() {
    return comentarios;
  }
}
