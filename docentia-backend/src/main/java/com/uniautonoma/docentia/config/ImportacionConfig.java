package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.service.importacion.LectorCsv;
import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ImportacionProperties.class)
public class ImportacionConfig {

  @Bean
  LectorCsv lectorCsv(ImportacionProperties propiedades) {
    return new LectorCsv(propiedades.filasMaximas());
  }

  @Bean
  ValidadorEvaluacionEstudiantes validadorEvaluacionEstudiantes() {
    return new ValidadorEvaluacionEstudiantes();
  }
}
