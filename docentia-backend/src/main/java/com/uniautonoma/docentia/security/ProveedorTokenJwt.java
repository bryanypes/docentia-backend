package com.uniautonoma.docentia.security;

import com.uniautonoma.docentia.config.JwtProperties;
import com.uniautonoma.docentia.entity.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Emite y valida los tokens de sesión. El alcance del usuario viaja dentro del token. */
@Component
public class ProveedorTokenJwt {

  private static final int LONGITUD_MINIMA_SECRETO = 32;
  private static final String CLAIM_ID_USUARIO = "idUsuario";
  private static final String CLAIM_NOMBRE = "nombreCompleto";
  private static final String CLAIM_ROL = "rol";
  private static final String CLAIM_ID_FACULTAD = "idFacultad";
  private static final String CLAIM_ID_PROGRAMA = "idPrograma";
  private static final String CLAIM_CONTRASENA_TEMPORAL = "contrasenaTemporal";

  private final SecretKey llaveFirma;
  private final Duration vigencia;

  public ProveedorTokenJwt(JwtProperties propiedades) {
    byte[] secreto =
        propiedades.secreto() == null
            ? new byte[0]
            : propiedades.secreto().getBytes(StandardCharsets.UTF_8);
    if (secreto.length < LONGITUD_MINIMA_SECRETO) {
      throw new IllegalStateException(
          "docentia.jwt.secreto (DOCENTIA_JWT_SECRETO) debe tener al menos "
              + LONGITUD_MINIMA_SECRETO
              + " caracteres");
    }
    this.llaveFirma = Keys.hmacShaKeyFor(secreto);
    this.vigencia = Duration.ofMinutes(propiedades.minutosVigencia());
  }

  public String generarToken(UsuarioSesion sesion) {
    Instant ahora = Instant.now();
    return Jwts.builder()
        .subject(sesion.correo())
        .claim(CLAIM_ID_USUARIO, sesion.idUsuario())
        .claim(CLAIM_NOMBRE, sesion.nombreCompleto())
        .claim(CLAIM_ROL, sesion.rol().name())
        .claim(CLAIM_ID_FACULTAD, sesion.idFacultad())
        .claim(CLAIM_ID_PROGRAMA, sesion.idPrograma())
        .claim(CLAIM_CONTRASENA_TEMPORAL, sesion.debeCambiarContrasena())
        .issuedAt(Date.from(ahora))
        .expiration(Date.from(ahora.plus(vigencia)))
        .signWith(llaveFirma)
        .compact();
  }

  /**
   * @return la sesión contenida en el token, o vacío si la firma no coincide o ya venció
   */
  public Optional<UsuarioSesion> leerSesion(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(llaveFirma).build().parseSignedClaims(token).getPayload();
      return Optional.of(
          new UsuarioSesion(
              leerIdentificador(claims, CLAIM_ID_USUARIO),
              claims.get(CLAIM_NOMBRE, String.class),
              claims.getSubject(),
              Rol.valueOf(claims.get(CLAIM_ROL, String.class)),
              leerIdentificador(claims, CLAIM_ID_FACULTAD),
              leerIdentificador(claims, CLAIM_ID_PROGRAMA),
              Boolean.TRUE.equals(claims.get(CLAIM_CONTRASENA_TEMPORAL, Boolean.class))));
    } catch (JwtException | IllegalArgumentException excepcion) {
      return Optional.empty();
    }
  }

  // Los números pequeños llegan del JSON como Integer y los grandes como Long.
  private Long leerIdentificador(Claims claims, String nombreClaim) {
    Number valor = claims.get(nombreClaim, Number.class);
    return valor == null ? null : valor.longValue();
  }
}
