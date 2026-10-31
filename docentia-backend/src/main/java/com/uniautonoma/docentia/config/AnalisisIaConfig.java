package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.service.analisis.Anonimizador;
import com.uniautonoma.docentia.service.analisis.FiltroComentarios;
import com.uniautonoma.docentia.service.analisis.FiltroPorReglas;
import com.uniautonoma.docentia.service.analisis.InstruccionAnalisis;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis;
import com.uniautonoma.docentia.service.analisis.ModeloIA;
import com.uniautonoma.docentia.service.analisis.ModeloIAChatCompletions;
import com.uniautonoma.docentia.service.analisis.ModeloIASimulado;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

/** Piezas del análisis con IA. El modelo se elige con docentia.ia.proveedor. */
@Configuration
@EnableConfigurationProperties(IaProperties.class)
public class AnalisisIaConfig {

  @Bean
  @ConditionalOnProperty(
      prefix = "docentia.ia",
      name = "proveedor",
      havingValue = "simulado",
      matchIfMissing = true)
  ModeloIA modeloIASimulado() {
    return new ModeloIASimulado();
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "docentia.ia",
      name = "proveedor",
      havingValue = "chat-completions")
  ModeloIA modeloIAChatCompletions(RestClient.Builder restClientBuilder, IaProperties propiedades) {
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofSeconds(10));
    fabrica.setReadTimeout(Duration.ofSeconds(propiedades.timeoutSegundos()));
    return new ModeloIAChatCompletions(
        restClientBuilder.clone().requestFactory(fabrica), propiedades);
  }

  /**
   * Filtro de comentarios ofensivos o sobre rasgos personales. Las reglas están en un archivo
   * editable; con docentia.ia.filtro-reglas=file:/ruta/reglas.txt se usa uno externo.
   */
  @Bean
  FiltroComentarios filtroComentarios(
      ResourceLoader cargador,
      @Value("${docentia.ia.filtro-reglas:classpath:filtros/reglas-comentarios.txt}") String ruta) {
    return new FiltroPorReglas(cargador.getResource(ruta));
  }

  @Bean
  Anonimizador anonimizador() {
    return new Anonimizador();
  }

  @Bean
  LectorRespuestaAnalisis lectorRespuestaAnalisis() {
    return new LectorRespuestaAnalisis();
  }

  @Bean
  InstruccionAnalisis instruccionAnalisis() {
    return InstruccionAnalisis.cargar();
  }

  /** El procesador en segundo plano solo corre si se habilita; las pruebas lo invocan a mano. */
  @Configuration
  @EnableScheduling
  @ConditionalOnProperty(prefix = "docentia.ia.worker", name = "habilitado", havingValue = "true")
  static class Programacion {}
}
