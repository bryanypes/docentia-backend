package com.uniautonoma.docentia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docentia.google")
public record GoogleProperties(String clientId) {

  public boolean habilitado() {
    return clientId != null && !clientId.isBlank();
  }
}
