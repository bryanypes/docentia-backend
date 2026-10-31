package com.uniautonoma.docentia.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Da un identificador a cada petición; va en los logs, en la cabecera y en los errores. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroCorrelacion extends OncePerRequestFilter {

  public static final String CLAVE_MDC = "correlationId";
  public static final String CABECERA = "X-Correlation-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // Se genera aquí y no se acepta del cliente, para que no pueda inyectar texto en los logs.
    String id = UUID.randomUUID().toString();
    MDC.put(CLAVE_MDC, id);
    response.setHeader(CABECERA, id);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CLAVE_MDC);
    }
  }
}
