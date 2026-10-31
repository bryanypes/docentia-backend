package com.uniautonoma.docentia.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class FiltroAutenticacionJwt extends OncePerRequestFilter {

  private static final String PREFIJO_BEARER = "Bearer ";

  private final ProveedorTokenJwt proveedorTokenJwt;

  public FiltroAutenticacionJwt(ProveedorTokenJwt proveedorTokenJwt) {
    this.proveedorTokenJwt = proveedorTokenJwt;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (encabezado != null && encabezado.startsWith(PREFIJO_BEARER)) {
      proveedorTokenJwt
          .leerSesion(encabezado.substring(PREFIJO_BEARER.length()))
          .ifPresent(
              sesion ->
                  SecurityContextHolder.getContext().setAuthentication(sesion.aAutenticacion()));
    }
    filterChain.doFilter(request, response);
  }
}
