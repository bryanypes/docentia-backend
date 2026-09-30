package com.uniautonoma.docentia.exception;

import com.uniautonoma.docentia.dto.ErrorResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

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
}
