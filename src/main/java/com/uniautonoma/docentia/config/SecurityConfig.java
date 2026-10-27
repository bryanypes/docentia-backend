package com.uniautonoma.docentia.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniautonoma.docentia.dto.ErrorResponse;
import com.uniautonoma.docentia.security.FiltroAutenticacionJwt;
import com.uniautonoma.docentia.security.ProveedorTokenJwt;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

  private final ObjectMapper objectMapper;

  public SecurityConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  SecurityFilterChain cadenaFiltrosSeguridad(
      HttpSecurity http, ProveedorTokenJwt proveedorTokenJwt, CorsProperties corsProperties)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(fuenteCors(corsProperties)))
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

  /**
   * Sin orígenes configurados no se registra ninguna ruta: el navegador bloquea cualquier llamada
   * desde otro dominio. No se usan cookies, así que no se permiten credenciales.
   */
  static CorsConfigurationSource fuenteCors(CorsProperties corsProperties) {
    UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
    if (corsProperties.origenesPermitidos().isEmpty()) {
      return fuente;
    }
    CorsConfiguration configuracion = new CorsConfiguration();
    configuracion.setAllowedOrigins(corsProperties.origenesPermitidos());
    configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuracion.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
    configuracion.setAllowCredentials(false);
    configuracion.setMaxAge(3600L);
    fuente.registerCorsConfiguration("/**", configuracion);
    return fuente;
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
