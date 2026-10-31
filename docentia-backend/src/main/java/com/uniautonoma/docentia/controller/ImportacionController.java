package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.ImportacionResponse;
import com.uniautonoma.docentia.dto.ReporteImportacionResponse;
import com.uniautonoma.docentia.dto.ResultadoImportacionResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.importacion.ImportacionService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Carga manual del CSV de evaluación de estudiantes exportado de Power BI. */
@RestController
@RequestMapping("/importaciones")
public class ImportacionController {

  private final ImportacionService importacionService;

  public ImportacionController(ImportacionService importacionService) {
    this.importacionService = importacionService;
  }

  /**
   * Revisa el archivo sin guardar nada.
   *
   * @param archivo CSV en la parte "archivo" del formulario multipart
   * @param mapeo opcional: JSON columna → campo; reemplaza la sugerencia automática
   * @param plantilla opcional: nombre de una plantilla propia, si no se envía mapeo
   * @param sesion coordinador autenticado
   * @return columnas detectadas, mapeo, errores y vista previa; 200 aunque el archivo tenga errores
   */
  @PostMapping(value = "/csv/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('COORDINADOR')")
  public ReporteImportacionResponse validarCsv(
      @RequestPart("archivo") MultipartFile archivo,
      @RequestParam(required = false) String mapeo,
      @RequestParam(required = false) String plantilla,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return importacionService.validar(archivo, mapeo, plantilla, sesion);
  }

  /**
   * Importa el archivo en el periodo activo.
   *
   * @param archivo CSV en la parte "archivo" del formulario multipart
   * @param mapeo opcional: JSON columna → campo; reemplaza la sugerencia automática
   * @param plantilla opcional: nombre de una plantilla propia, si no se envía mapeo
   * @param sesion coordinador autenticado
   * @return 200 si se importó; 422 con el reporte si se rechazó y no se guardó nada
   */
  @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('COORDINADOR')")
  public ResponseEntity<ResultadoImportacionResponse> importarCsv(
      @RequestPart("archivo") MultipartFile archivo,
      @RequestParam(required = false) String mapeo,
      @RequestParam(required = false) String plantilla,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    ResultadoImportacionResponse resultado =
        importacionService.importar(archivo, mapeo, plantilla, sesion);
    HttpStatus estado = resultado.fueExitosa() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
    return ResponseEntity.status(estado).body(resultado);
  }

  /**
   * Lista las cargas: el coordinador ve las suyas y la vicerrectoría todas.
   *
   * @param idPeriodo filtro opcional por periodo
   * @param sesion usuario autenticado
   * @return cargas de la más reciente a la más antigua
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR')")
  public List<ImportacionResponse> listarImportaciones(
      @RequestParam(required = false) Long idPeriodo,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return importacionService.listarImportaciones(sesion, idPeriodo);
  }
}
