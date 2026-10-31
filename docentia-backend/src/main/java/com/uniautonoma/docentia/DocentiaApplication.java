package com.uniautonoma.docentia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// La autenticación es por JWT propio; sin esta exclusión Spring crea un usuario en memoria.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class DocentiaApplication {

  public static void main(String[] args) {
    SpringApplication.run(DocentiaApplication.class, args);
  }
}
