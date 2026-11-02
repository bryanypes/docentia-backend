package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.CoberturaHeteroevaluacionResponse;
import com.uniautonoma.docentia.dto.EnvioEstudianteRequest;
import com.uniautonoma.docentia.dto.EvaluacionesEstudianteResponse;
import com.uniautonoma.docentia.dto.FormularioEstudianteResponse;
import com.uniautonoma.docentia.dto.ResultadoMatriculaResponse;
import com.uniautonoma.docentia.entity.AccionAuditoria;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.AuditoriaService;
import com.uniautonoma.docentia.service.heteroevaluacion.CoberturaService;
import com.uniautonoma.docentia.service.heteroevaluacion.HeteroevaluacionService;
import com.uniautonoma.docentia.service.heteroevaluacion.MatriculaService;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Heteroevaluación: formulario del estudiante, matrícula y cobertura. */
@RestController
public class HeteroevaluacionController {

  private final HeteroevaluacionService heteroevaluacion;
  private final MatriculaService matricula;
  private final CoberturaService cobertura;
  private final AuditoriaService auditoria;

  public HeteroevaluacionController(
      HeteroevaluacionService heteroevaluacion,
      MatriculaService matricula,
      CoberturaService cobertura,
      AuditoriaService auditoria) {
    this.heteroevaluacion = heteroevaluacion;
    this.matricula = matricula;
    this.cobertura = cobertura;
    this.auditoria = auditoria;
  }

  @GetMapping("/estudiante/evaluaciones")
  @PreAuthorize("hasRole('ESTUDIANTE')")
  public EvaluacionesEstudianteResponse listar(
      @RequestParam(required = false) Long periodo, @AuthenticationPrincipal UsuarioSesion sesion) {
    return heteroevaluacion.listar(periodo, sesion);
  }

  @GetMapping("/estudiante/evaluaciones/{idAsignacion}")
  @PreAuthorize("hasRole('ESTUDIANTE')")
  public FormularioEstudianteResponse leer(
      @PathVariable Long idAsignacion, @AuthenticationPrincipal UsuarioSesion sesion) {
    return heteroevaluacion.leer(idAsignacion, sesion);
  }

  /** Solo confirma el envío: nunca devuelve lo respondido. */
  @PostMapping("/estudiante/evaluaciones/{idAsignacion}/enviar")
  @PreAuthorize("hasRole('ESTUDIANTE')")
  public Map<String, String> enviar(
      @PathVariable Long idAsignacion,
      @RequestBody EnvioEstudianteRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    heteroevaluacion.enviar(idAsignacion, solicitud, sesion);
    auditoria.registrar(AccionAuditoria.ENVIO_HETEROEVALUACION, "ASIGNACION", idAsignacion, sesion);
    return Map.of("estado", "ENVIADA");
  }

  @GetMapping("/cobertura-heteroevaluacion")
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR')")
  public CoberturaHeteroevaluacionResponse cobertura(
      @RequestParam(required = false) Long periodo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return cobertura.calcular(periodo, sesion, page, size);
  }

  @PostMapping(value = "/matriculas/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR')")
  public ResponseEntity<ResultadoMatriculaResponse> importar(
      @RequestPart("archivo") MultipartFile archivo,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    ResultadoMatriculaResponse resultado = matricula.importar(archivo, sesion);
    auditoria.registrar(
        AccionAuditoria.IMPORTACION_MATRICULA,
        "MATRICULA",
        null,
        sesion.idUsuario(),
        sesion.correo(),
        sesion.rol().name(),
        resultado.valido() ? AuditoriaService.EXITO : AuditoriaService.FALLO);
    return ResponseEntity.status(
            resultado.valido() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY)
        .body(resultado);
  }

  @GetMapping("/matriculas/plantilla")
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR')")
  public ResponseEntity<String> plantilla() {
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla-matricula.csv")
        .body(MatriculaService.PLANTILLA);
  }
}
