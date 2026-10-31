package com.uniautonoma.docentia.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.uniautonoma.docentia.config.GoogleProperties;
import com.uniautonoma.docentia.exception.CredencialesInvalidasException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class VerificadorIdTokenGoogle implements VerificadorTokenGoogle {

  private final GoogleIdTokenVerifier verificador;

  public VerificadorIdTokenGoogle(GoogleProperties googleProperties) {
    this.verificador =
        new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(
                googleProperties.habilitado()
                    ? Collections.singletonList(googleProperties.clientId())
                    : Collections.emptyList())
            .build();
  }

  @Override
  public String validarYExtraerCorreo(String idToken) {
    try {
      GoogleIdToken token = verificador.verify(idToken);
      if (token == null) {
        throw new CredencialesInvalidasException();
      }
      GoogleIdToken.Payload carga = token.getPayload();
      Object correo = carga.get("email");
      if (!(correo instanceof String texto) || texto.isBlank()) {
        throw new CredencialesInvalidasException();
      }
      if (!Boolean.TRUE.equals(carga.getEmailVerified())) {
        throw new CredencialesInvalidasException();
      }
      return texto.trim().toLowerCase(Locale.ROOT);
    } catch (GeneralSecurityException | IOException excepcion) {
      throw new CredencialesInvalidasException();
    }
  }
}
