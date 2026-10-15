package com.uniautonoma.docentia.exception;

import com.uniautonoma.docentia.dto.ErrorResponse;
import com.uniautonoma.docentia.dto.EtapaCerradaResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

  private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

  @ExceptionHandler(RecursoNoEncontradoException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse manejarRecursoNoEncontrado(RecursoNoEncontradoException excepcion) {
    return new ErrorResponse(excepcion.getMessage());
  }

  @ExceptionHandler(ReglaNegocioException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse manejarReglaNegocio(ReglaNegocioException excepcion) {
    return new ErrorResponse(excepcion.getMessage());
  }

  @ExceptionHandler(EtapaCerradaException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public EtapaCerradaResponse manejarEtapaCerrada(EtapaCerradaException excepcion) {
    return EtapaCerradaResponse.desde(excepcion);
  }

  /**
   * Una restricción de la base rechazó la escritura, casi siempre porque otra persona guardó el
   * mismo dato al mismo tiempo. El detalle de la base no se envía al cliente.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse manejarConflictoDeDatos(DataIntegrityViolationException excepcion) {
    LOG.warn("Escritura rechazada por la base: {}", excepcion.getMostSpecificCause().getMessage());
    return new ErrorResponse(
        "Otra persona modificó estos datos al mismo tiempo; vuelve a intentarlo");
  }

  @ExceptionHandler(CredencialesInvalidasException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ErrorResponse manejarCredencialesInvalidas(CredencialesInvalidasException excepcion) {
    return new ErrorResponse(excepcion.getMessage());
  }

  @ExceptionHandler(DatosInvalidosException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse manejarDatosInvalidos(DatosInvalidosException excepcion) {
    return new ErrorResponse(excepcion.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse manejarCamposInvalidos(MethodArgumentNotValidException excepcion) {
    Map<String, String> errores = new LinkedHashMap<>();
    for (FieldError error : excepcion.getBindingResult().getFieldErrors()) {
      errores.putIfAbsent(error.getField(), error.getDefaultMessage());
    }
    return new ErrorResponse("Hay campos con datos inválidos", errores);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse manejarCuerpoIlegible(HttpMessageNotReadableException excepcion) {
    return new ErrorResponse("El cuerpo de la petición no se pudo interpretar");
  }

  @ExceptionHandler(TypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse manejarParametroInvalido(TypeMismatchException excepcion) {
    return new ErrorResponse(
        "El parámetro " + excepcion.getPropertyName() + " no tiene un valor válido");
  }

  @ExceptionHandler({ArchivoDemasiadoGrandeException.class, MaxUploadSizeExceededException.class})
  @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
  public ErrorResponse manejarArchivoDemasiadoGrande(Exception excepcion) {
    return new ErrorResponse(
        excepcion instanceof ArchivoDemasiadoGrandeException
            ? excepcion.getMessage()
            : "El archivo supera el tamaño permitido");
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse manejarParteFaltante(MissingServletRequestPartException excepcion) {
    return new ErrorResponse(
        "Falta la parte \"" + excepcion.getRequestPartName() + "\" del formulario");
  }

  /**
   * Último recurso: nunca se envía la traza al cliente. Las excepciones de seguridad se relanzan
   * para que Spring Security responda 401 o 403, y las propias de Spring MVC conservan su código.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> manejarInesperado(Exception excepcion) throws Exception {
    if (excepcion instanceof AccessDeniedException
        || excepcion instanceof AuthenticationException) {
      throw excepcion;
    }
    if (excepcion instanceof org.springframework.web.ErrorResponse respuestaSpring) {
      String detalle = respuestaSpring.getBody().getDetail();
      return ResponseEntity.status(respuestaSpring.getStatusCode())
          .body(new ErrorResponse(detalle != null ? detalle : "La petición no se pudo atender"));
    }
    LOG.error("Error no controlado", excepcion);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("Ocurrió un error inesperado; intenta de nuevo"));
  }
}
