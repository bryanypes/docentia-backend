package com.uniautonoma.docentia.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniautonoma.docentia.dto.ErrorResponse;
import com.uniautonoma.docentia.security.FiltroAutenticacionJwt;
import com.uniautonoma.docentia.security.ProveedorTokenJwt;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  private final ObjectMapper objectMapper;

  public SecurityConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  SecurityFilterChain cadenaFiltrosSeguridad(HttpSecurity http, ProveedorTokenJwt proveedorTokenJwt)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            peticiones ->
                peticiones
                    .requestMatchers(HttpMethod.POST, "/sesiones")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errores ->
                errores
                    .authenticationEntryPoint(
                        (request, response, excepcion) ->
                            escribirError(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "Debes iniciar sesión para continuar"))
                    .accessDeniedHandler(
                        (request, response, excepcion) ->
                            escribirError(
                                response,
                                HttpStatus.FORBIDDEN,
                                "No tienes permiso para realizar esta acción")))
        .addFilterBefore(
            new FiltroAutenticacionJwt(proveedorTokenJwt),
            UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private void escribirError(HttpServletResponse response, HttpStatus estado, String mensaje)
      throws IOException {
    response.setStatus(estado.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getWriter(), new ErrorResponse(mensaje));
  }
}
