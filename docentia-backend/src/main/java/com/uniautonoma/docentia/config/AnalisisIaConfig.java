package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.service.analisis.Anonimizador;
import com.uniautonoma.docentia.service.analisis.FiltroComentarios;
import com.uniautonoma.docentia.service.analisis.FiltroPorReglas;
import com.uniautonoma.docentia.service.analisis.InstruccionAnalisis;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis;
import com.uniautonoma.docentia.service.analisis.ModeloIA;
import com.uniautonoma.docentia.service.analisis.ModeloIAChatCompletions;
import com.uniautonoma.docentia.service.analisis.ModeloIASimulado;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal.ParametrosAnalisis;
import com.uniautonoma.docentia.service.analisis.local.FiltroCalidad;
import com.uniautonoma.docentia.service.analisis.local.Lexico;
import com.uniautonoma.docentia.service.analisis.local.ModeloIALocal;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis;
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
@EnableConfigurationProperties({
  IaProperties.class,
  AnalisisComentariosProperties.class,
  AhorroIaProperties.class
})
public class AnalisisIaConfig {

  /** Modelo local por reglas: el de por defecto, sin red ni datos fuera de la institución. */
  @Bean
  @ConditionalOnProperty(
      prefix = "docentia.ia",
      name = "proveedor",
      havingValue = "local",
      matchIfMissing = true)
  ModeloIA modeloIALocal(AnalizadorLocal analizador) {
    return new ModeloIALocal(analizador);
  }

  @Bean
  Lexico lexicoAnalisis(AnalisisComentariosProperties propiedades) {
    return Lexico.cargar(propiedades.lexico());
  }

  @Bean
  AnalizadorLocal analizadorLocal(
      Lexico lexico,
      AnalisisComentariosProperties propiedades,
      HeteroevaluacionProperties heteroevaluacion) {
    return new AnalizadorLocal(
        lexico,
        new ParametrosAnalisis(
            propiedades.minimoRecurrencia(),
            propiedades.porcentajeRecurrencia(),
            propiedades.maximoPorLista(),
            propiedades.umbralTendencia(),
            heteroevaluacion.minimoRespuestas()));
  }

  @Bean
  PipelineAnalisis pipelineAnalisis(
      Lexico lexico,
      AnalisisComentariosProperties propiedades,
      FiltroComentarios filtro,
      Anonimizador anonimizador) {
    return new PipelineAnalisis(
        new FiltroCalidad(lexico, propiedades.maximoRepeticiones()), filtro, anonimizador);
  }

  /** Doble de pruebas: imita a un modelo de lenguaje sin red. */
  @Bean
  @ConditionalOnProperty(prefix = "docentia.ia", name = "proveedor", havingValue = "simulado")
  ModeloIA modeloIASimulado() {
    return new ModeloIASimulado();
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "docentia.ia",
      name = "proveedor",
      havingValue = "chat-completions")
  ModeloIA modeloIAChatCompletions(
      RestClient.Builder restClientBuilder, IaProperties propiedades, AhorroIaProperties ahorro) {
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofSeconds(10));
    fabrica.setReadTimeout(Duration.ofSeconds(propiedades.timeoutSegundos()));
    return new ModeloIAChatCompletions(
        restClientBuilder.clone().requestFactory(fabrica), propiedades, ahorro.maxTokensSalida());
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
