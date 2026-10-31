package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.ActaResponse;
import com.uniautonoma.docentia.dto.CompromisoActaRequest;
import com.uniautonoma.docentia.dto.CorreccionActaRequest;
import com.uniautonoma.docentia.dto.FirmaActaRequest;
import com.uniautonoma.docentia.dto.NuevaActaRequest;
import com.uniautonoma.docentia.entity.AccionAuditoria;
import com.uniautonoma.docentia.entity.EstadoActa;
import com.uniautonoma.docentia.security.DireccionCliente;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.ActaService;
import com.uniautonoma.docentia.service.ActaService.PdfActa;
import com.uniautonoma.docentia.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Actas de retroalimentación: borrador, compromisos, firma, PDF y corrección por versiones. */
@RestController
@RequestMapping("/actas")
public class ActaController {

  private final ActaService actaService;
  private final AuditoriaService auditoria;

  public ActaController(ActaService actaService, AuditoriaService auditoria) {
    this.actaService = actaService;
    this.auditoria = auditoria;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('COORDINADOR')")
  public ActaResponse crear(
      @Valid @RequestBody NuevaActaRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return actaService.crear(solicitud.idDocente(), sesion);
  }

  @GetMapping
  public List<ActaResponse> listar(
      @RequestParam(required = false) Long idDocente,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return actaService.listar(idDocente, sesion);
  }

  @GetMapping("/{idActa}")
  public ActaResponse obtener(
      @PathVariable Long idActa, @AuthenticationPrincipal UsuarioSesion sesion) {
    ActaResponse acta = actaService.obtener(idActa, sesion);
    auditarLectura(acta, sesion);
    return acta;
  }

  @PostMapping("/{idActa}/compromisos")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('COORDINADOR')")
  public ActaResponse agregarCompromiso(
      @PathVariable Long idActa,
      @Valid @RequestBody CompromisoActaRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return actaService.agregarCompromiso(idActa, solicitud, sesion);
  }

  @DeleteMapping("/{idActa}/compromisos/{idCompromiso}")
  @PreAuthorize("hasRole('COORDINADOR')")
  public ActaResponse quitarCompromiso(
      @PathVariable Long idActa,
      @PathVariable Long idCompromiso,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return actaService.quitarCompromiso(idActa, idCompromiso, sesion);
  }

  /** Firma de docente o coordinación. La IP real es la última que agregó el proxy propio. */
  @PostMapping("/{idActa}/firmas")
  @PreAuthorize("hasAnyRole('COORDINADOR', 'DOCENTE')")
  public ActaResponse firmar(
      @PathVariable Long idActa,
      @RequestBody FirmaActaRequest solicitud,
      HttpServletRequest peticion,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    ActaResponse acta =
        actaService.firmar(idActa, solicitud.confirmacion(), DireccionCliente.de(peticion), sesion);
    auditoria.registrar(AccionAuditoria.FIRMA_ACTA, "ACTA", idActa, sesion);
    return acta;
  }

  @PostMapping("/{idActa}/correcciones")
  @ResponseStatus(HttpStatus.CREATED)
  public ActaResponse corregir(
      @PathVariable Long idActa,
      @Valid @RequestBody CorreccionActaRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    ActaResponse acta = actaService.corregir(idActa, solicitud.motivo(), sesion);
    auditoria.registrar(AccionAuditoria.CORRECCION_ACTA, "ACTA", idActa, sesion);
    return acta;
  }

  @GetMapping("/{idActa}/pdf")
  public ResponseEntity<byte[]> descargarPdf(
      @PathVariable Long idActa, @AuthenticationPrincipal UsuarioSesion sesion) {
    PdfActa pdf = actaService.descargarPdf(idActa, sesion);
    if (!sesion.esDocente()) {
      auditoria.registrar(AccionAuditoria.LECTURA_ACTA_FIRMADA, "ACTA", idActa, sesion);
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(pdf.nombre()).build().toString())
        .body(pdf.contenido());
  }

  private void auditarLectura(ActaResponse acta, UsuarioSesion sesion) {
    if (acta.estado() == EstadoActa.FIRMADA && !sesion.esDocente()) {
      auditoria.registrar(AccionAuditoria.LECTURA_ACTA_FIRMADA, "ACTA", acta.idActa(), sesion);
    }
  }
}
