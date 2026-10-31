package com.uniautonoma.docentia.exception;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniautonoma.docentia.dto.ErrorResponse;
import com.uniautonoma.docentia.dto.EtapaCerradaResponse;
import java.util.List;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Convierte los errores en ProblemDetail y aplica el tope de los listados, en un solo lugar. */
@RestControllerAdvice
public class AsesorProblemas implements ResponseBodyAdvice<Object> {

  /** Ningún listado devuelve más filas; las bandejas grandes usan paginación real. */
  static final int TOPE_LISTADOS = 1000;

  private final ObjectMapper objectMapper;

  public AsesorProblemas(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(
      MethodParameter retorno, Class<? extends HttpMessageConverter<?>> convertidor) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object cuerpo,
      MethodParameter retorno,
      MediaType tipo,
      Class<? extends HttpMessageConverter<?>> convertidor,
      ServerHttpRequest peticion,
      ServerHttpResponse respuesta) {
    if (cuerpo instanceof List<?> lista && lista.size() > TOPE_LISTADOS) {
      respuesta.getHeaders().set("X-Total-Count", String.valueOf(lista.size()));
      return lista.subList(0, TOPE_LISTADOS);
    }
    if (!(cuerpo instanceof ErrorResponse) && !(cuerpo instanceof EtapaCerradaResponse)) {
      return cuerpo;
    }
    HttpStatus estado =
        respuesta instanceof ServletServerHttpResponse servlet
            ? HttpStatus.valueOf(servlet.getServletResponse().getStatus())
            : HttpStatus.INTERNAL_SERVER_ERROR;
    respuesta.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    return Problemas.crear(
        estado, objectMapper.convertValue(cuerpo, new TypeReference<Map<String, Object>>() {}));
  }
}
