package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.FormularioRequest;
import com.uniautonoma.docentia.dto.FormularioResponse;
import com.uniautonoma.docentia.dto.PendientesFormularioResponse;
import com.uniautonoma.docentia.entity.AccionAuditoria;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.AuditoriaService;
import com.uniautonoma.docentia.service.formulario.FormularioService;
import java.util.function.Supplier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Formularios de autoevaluación y coevaluación del Anexo 1. */
@RestController
@RequestMapping("/formularios")
public class FormularioController {

  private final FormularioService formularioService;
  private final AuditoriaService auditoria;

  public FormularioController(FormularioService formularioService, AuditoriaService auditoria) {
    this.formularioService = formularioService;
    this.auditoria = auditoria;
  }

  @GetMapping("/autoevaluacion")
  @PreAuthorize("hasRole('DOCENTE')")
  public FormularioResponse leerAutoevaluacion(
      @RequestParam(required = false) Long periodo, @AuthenticationPrincipal UsuarioSesion sesion) {
    return formularioService.leerAutoevaluacion(periodo, sesion);
  }

  @PutMapping("/autoevaluacion")
  @PreAuthorize("hasRole('DOCENTE')")
  public FormularioResponse guardarAutoevaluacion(
      @RequestParam(required = false) Long periodo,
      @RequestBody FormularioRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return formularioService.guardarAutoevaluacion(periodo, solicitud, sesion);
  }

  @PostMapping("/autoevaluacion/enviar")
  @PreAuthorize("hasRole('DOCENTE')")
  public FormularioResponse enviarAutoevaluacion(
      @RequestParam(required = false) Long periodo, @AuthenticationPrincipal UsuarioSesion sesion) {
    return auditar(
        TipoFormulario.AUTO, sesion, () -> formularioService.enviarAutoevaluacion(periodo, sesion));
  }

  @GetMapping("/coevaluacion/pendientes")
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR', 'ADMINISTRADOR')")
  public PendientesFormularioResponse pendientes(
      @RequestParam(required = false) Long periodo, @AuthenticationPrincipal UsuarioSesion sesion) {
    return formularioService.pendientes(periodo, sesion);
  }

  @GetMapping("/coevaluacion/{idDocente}")
  @PreAuthorize("hasRole('COORDINADOR')")
  public FormularioResponse leerCoevaluacion(
      @PathVariable Long idDocente,
      @RequestParam(required = false) Long periodo,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return formularioService.leerCoevaluacion(periodo, idDocente, sesion);
  }

  @PutMapping("/coevaluacion/{idDocente}")
  @PreAuthorize("hasRole('COORDINADOR')")
  public FormularioResponse guardarCoevaluacion(
      @PathVariable Long idDocente,
      @RequestParam(required = false) Long periodo,
      @RequestBody FormularioRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return formularioService.guardarCoevaluacion(periodo, idDocente, solicitud, sesion);
  }

  @PostMapping("/coevaluacion/{idDocente}/enviar")
  @PreAuthorize("hasRole('COORDINADOR')")
  public FormularioResponse enviarCoevaluacion(
      @PathVariable Long idDocente,
      @RequestParam(required = false) Long periodo,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return auditar(
        TipoFormulario.COEVAL,
        sesion,
        () -> formularioService.enviarCoevaluacion(periodo, idDocente, sesion));
  }

  /** Deja rastro del envío, también si falla. Nunca lleva puntajes. */
  private FormularioResponse auditar(
      TipoFormulario tipo, UsuarioSesion sesion, Supplier<FormularioResponse> envio) {
    String entidad = "FORMULARIO_" + tipo;
    try {
      FormularioResponse enviado = envio.get();
      registrar(entidad, enviado.idDocente(), sesion, AuditoriaService.EXITO);
      return enviado;
    } catch (RuntimeException rechazo) {
      registrar(entidad, null, sesion, AuditoriaService.FALLO);
      throw rechazo;
    }
  }

  private void registrar(String entidad, Long idDocente, UsuarioSesion sesion, String resultado) {
    auditoria.registrar(
        AccionAuditoria.ENVIO_FORMULARIO,
        entidad,
        idDocente,
        sesion.idUsuario(),
        sesion.correo(),
        sesion.rol().name(),
        resultado);
  }
}
