# Documentación de DOCENTIA

Índice de esta carpeta. Lo que ya no describe el sistema actual está en [historico/](historico/).

## Para empezar

| Documento | Qué tiene |
|---|---|
| [ARQUITECTURA.md](ARQUITECTURA.md) | Componentes, base de datos y decisiones de diseño |
| [OPERACION.md](OPERACION.md) | Variables de entorno, arranque, respaldo y vuelta atrás |
| [SEGURIDAD.md](SEGURIDAD.md) | Autenticación, autorización, datos personales (Ley 1581) |
| [ROLES.md](ROLES.md) | Qué puede hacer cada rol y en qué rutas de la API |
| [LIMITACIONES.md](LIMITACIONES.md) | Lo que el sistema no hace y por qué |
| [manual/](manual/README.md) | Manual de uso por rol |

## Despliegue

| Documento | Qué tiene |
|---|---|
| [DESPLIEGUE-0.4.0.md](DESPLIEGUE-0.4.0.md) | Paso de 0.3.3 a 0.4.0: respaldo, migraciones V34 a V43, primer administrador |
| [DESPLIEGUE-0.4.1.md](DESPLIEGUE-0.4.1.md) | Paso a 0.4.1 y qué commit lleva cada repositorio |
| [DESPLIEGUE-0.4.2.md](DESPLIEGUE-0.4.2.md) | Paso a 0.4.2 |
| [consultas-verificacion.sql](consultas-verificacion.sql) | Consultas de solo lectura para antes y después de desplegar |

## API

| Documento | Qué tiene |
|---|---|
| [API.md](API.md) | Índice de la API y convenciones |
| [api-contrato-frontend.md](api-contrato-frontend.md) | Contrato general con el frontend |
| [api-sprint3-semana1.md](api-sprint3-semana1.md) | Periodos, calificaciones y etapas (la parte de CSV ya no existe) |
| [api-sprint3-semana2.md](api-sprint3-semana2.md) | Agenda e indicadores |
| [api-proceso-evaluacion.md](api-proceso-evaluacion.md) | Estado del proceso, cita y asistencia |
| [api-heteroevaluacion.md](api-heteroevaluacion.md) | Evaluación del estudiante al docente |
| [api-comentarios-ia.md](api-comentarios-ia.md) | Análisis de comentarios |
| [api-res074.md](api-res074.md) | Reglas de la Resolución 074 |
| [api-sesiones-google.md](api-sesiones-google.md) | Inicio de sesión con Google |
| [ejemplos/ia/](ejemplos/ia/) | Respuestas reales del análisis, generadas por las pruebas |

## Decisiones y trazabilidad

| Documento | Qué tiene |
|---|---|
| [DECISIONES.md](DECISIONES.md) | Decisiones del proyecto, con quién decide |
| [DECISIONES-HETEROEVALUACION.md](DECISIONES-HETEROEVALUACION.md) | Heteroevaluación anónima |
| [DECISIONES-ANALISIS-IA.md](DECISIONES-ANALISIS-IA.md) | Análisis de comentarios y fallos del proveedor externo |
| [RES074-TRAZABILIDAD.md](RES074-TRAZABILIDAD.md) | De cada artículo de la resolución a su código y su prueba |
| [FIRMA-ACTAS.md](FIRMA-ACTAS.md) | Cómo funciona la firma de actas |
| [NOTIFICACIONES.md](NOTIFICACIONES.md) | Avisos y correos |

## Análisis de comentarios

| Documento | Qué tiene |
|---|---|
| [ANALISIS_IA_CRITERIOS.md](ANALISIS_IA_CRITERIOS.md) | Los seis criterios y cómo se etiquetan |
| [ANALISIS_IA_VALIDACION.md](ANALISIS_IA_VALIDACION.md) | Cómo se mide el modelo local y su robustez |
| [ANALISIS_IA_COSTOS.md](ANALISIS_IA_COSTOS.md) | Gasto de tokens del proveedor externo |
| [integracion-analisis-ia.md](integracion-analisis-ia.md) | De dónde sale cada dato del análisis |
| [recorrido-analisis-ia/FLUJO.md](recorrido-analisis-ia/FLUJO.md) | Recorrido del análisis del formulario |
| [recorrido-heteroevaluacion/FLUJO.md](recorrido-heteroevaluacion/FLUJO.md) | Recorrido de la evaluación del estudiante, con capturas |

## Sustentación

| Documento | Qué tiene |
|---|---|
| [sustentacion/RESUMEN_EJECUTIVO.md](sustentacion/RESUMEN_EJECUTIVO.md) | Resumen para la institución |
| [sustentacion/GUIA_TECNICA.md](sustentacion/GUIA_TECNICA.md) | Guía técnica |
| [sustentacion/GUION_PRUEBA_Y_DEMO.md](sustentacion/GUION_PRUEBA_Y_DEMO.md) | Guion de prueba y demostración |

## Referencia institucional

- `Resolucion 074_13abr2026_MODELO AUTOEVALUACIÓN ENSEÑANZA.pdf`: la resolución.
- `Requerimientos_Modelo_Evaluacion_Ensenanza_Res074_v2.xlsx`: requerimientos derivados.
