package com.uniautonoma.docentia.entity;

import com.uniautonoma.docentia.dto.EstadoPromedio;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Acta de retroalimentación de un docente en un periodo. Copia el promedio y las notas para que una
 * corrección posterior no la cambie. Firmada es inmutable: corregirla crea una versión nueva.
 */
@Entity
@Table(name = "actas")
public class Acta {

  @Version
  @Column(name = "version_bloqueo", nullable = false)
  private long versionBloqueo;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idActa;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_docente", nullable = false, updatable = false)
  private Usuario docente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @Column(nullable = false, updatable = false)
  private int version = 1;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoActa estado = EstadoActa.BORRADOR;

  @Column(precision = 3, scale = 2)
  private BigDecimal promedio;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoPromedio estadoPromedio;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private CategoriaDesempeno categoria;

  @Column(precision = 5, scale = 3)
  private BigDecimal notaEstudiantes;

  @Column(precision = 5, scale = 3)
  private BigDecimal notaCoordinacion;

  @Column(precision = 5, scale = 3)
  private BigDecimal notaAutoevaluacion;

  @Column(nullable = false, precision = 3, scale = 2)
  private BigDecimal umbralCompromisos;

  private Long idUltimaCorreccion;

  private Long idActaAnterior;

  @Column(length = 500)
  private String motivoCorreccion;

  private Long idUsuarioCorrige;

  @Column(length = 20)
  private String codigoVerificacion;

  @Column(length = 64)
  private String hashContenido;

  @Column(length = 64)
  private String hashPdf;

  @Column(columnDefinition = "bytea")
  private byte[] pdf;

  private LocalDateTime fechaFirma;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  @OneToMany(mappedBy = "acta", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("fecha asc")
  private final List<FirmaActa> firmas = new ArrayList<>();

  @OneToMany(mappedBy = "acta", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("fechaLimite asc")
  private final List<Compromiso> compromisos = new ArrayList<>();

  protected Acta() {}

  public Acta(Usuario docente, PeriodoAcademico periodo, BigDecimal umbralCompromisos) {
    this.docente = docente;
    this.periodo = periodo;
    this.umbralCompromisos = umbralCompromisos;
  }

  /** Crea la versión siguiente de un acta firmada, con sus compromisos como punto de partida. */
  public static Acta nuevaVersion(Acta anterior, String motivo, Long idUsuarioCorrige) {
    Acta nueva = new Acta(anterior.docente, anterior.periodo, anterior.umbralCompromisos);
    nueva.version = anterior.version + 1;
    nueva.idActaAnterior = anterior.idActa;
    nueva.motivoCorreccion = motivo;
    nueva.idUsuarioCorrige = idUsuarioCorrige;
    for (Compromiso compromiso : anterior.compromisos) {
      nueva.agregarCompromiso(compromiso.getDescripcion(), compromiso.getFechaLimite());
    }
    return nueva;
  }

  /** Copia el resultado vigente de la evaluación. Solo mientras nadie haya firmado. */
  public void congelarResultado(
      BigDecimal promedio,
      EstadoPromedio estadoPromedio,
      CategoriaDesempeno categoria,
      BigDecimal notaEstudiantes,
      BigDecimal notaCoordinacion,
      BigDecimal notaAutoevaluacion,
      Long idUltimaCorreccion) {
    if (!firmas.isEmpty()) {
      return;
    }
    this.promedio = promedio;
    this.estadoPromedio = estadoPromedio;
    this.categoria = categoria;
    this.notaEstudiantes = notaEstudiantes;
    this.notaCoordinacion = notaCoordinacion;
    this.notaAutoevaluacion = notaAutoevaluacion;
    this.idUltimaCorreccion = idUltimaCorreccion;
  }

  public void exigirBorrador() {
    if (estado != EstadoActa.BORRADOR) {
      throw new ReglaNegocioException("El acta firmada no se puede modificar");
    }
  }

  public Compromiso agregarCompromiso(String descripcion, LocalDate fechaLimite) {
    Compromiso compromiso = new Compromiso(this, descripcion, fechaLimite);
    compromisos.add(compromiso);
    return compromiso;
  }

  public void quitarCompromiso(Compromiso compromiso) {
    compromisos.remove(compromiso);
  }

  public boolean requiereCompromisos() {
    return promedio != null && promedio.compareTo(umbralCompromisos) < 0;
  }

  public boolean firmadaPor(Rol rol) {
    return firmas.stream().anyMatch(firma -> firma.getRol() == rol);
  }

  public void registrarFirma(FirmaActa firma) {
    firmas.add(firma);
  }

  /** Cierra el acta con su PDF. Se llama una sola vez, con las dos firmas ya registradas. */
  public void cerrar(
      String codigoVerificacion,
      String hashContenido,
      String hashPdf,
      byte[] pdf,
      LocalDateTime fechaFirma) {
    this.codigoVerificacion = codigoVerificacion;
    this.hashContenido = hashContenido;
    this.hashPdf = hashPdf;
    this.pdf = pdf;
    this.fechaFirma = fechaFirma;
    this.estado = EstadoActa.FIRMADA;
  }

  public void anular() {
    this.estado = EstadoActa.ANULADA;
  }

  public Long getIdActa() {
    return idActa;
  }

  public Usuario getDocente() {
    return docente;
  }

  public PeriodoAcademico getPeriodo() {
    return periodo;
  }

  public int getVersion() {
    return version;
  }

  public EstadoActa getEstado() {
    return estado;
  }

  public BigDecimal getPromedio() {
    return promedio;
  }

  public EstadoPromedio getEstadoPromedio() {
    return estadoPromedio;
  }

  public CategoriaDesempeno getCategoria() {
    return categoria;
  }

  public BigDecimal getUmbralCompromisos() {
    return umbralCompromisos;
  }

  public Long getIdActaAnterior() {
    return idActaAnterior;
  }

  public String getMotivoCorreccion() {
    return motivoCorreccion;
  }

  public String getCodigoVerificacion() {
    return codigoVerificacion;
  }

  public String getHashContenido() {
    return hashContenido;
  }

  public String getHashPdf() {
    return hashPdf;
  }

  public byte[] getPdf() {
    return pdf;
  }

  public LocalDateTime getFechaFirma() {
    return fechaFirma;
  }

  public LocalDateTime getFechaCreacion() {
    return fechaCreacion;
  }

  public List<FirmaActa> getFirmas() {
    return firmas;
  }

  public List<Compromiso> getCompromisos() {
    return compromisos;
  }
}
