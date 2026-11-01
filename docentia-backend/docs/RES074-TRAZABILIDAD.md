# Trazabilidad con la Resolución 074 de 2026

Historias del Excel v2 frente a lo construido. Si difieren, manda la resolución.

| Historia | Estado | Evidencia |
|----------|--------|-----------|
| HU-01 Formulario de estudiantes | Por CSV; formulario futuro | DOCENTIA importa la heteroevaluación (`ImportacionCsvTest`); no hay rol de estudiante ni matrículas |
| HU-02 Formulario de coevaluación (6 ítems) | Implementado | `/formularios/coevaluacion`, `FormulariosTest`, E2E |
| HU-03 Formulario de autoevaluación (14 ítems) | Implementado | `/formularios/autoevaluacion`, `FormulariosTest`, E2E |
| HU-04 Cálculo de la nota final | Implementado | `CalculadoraPromedioPonderado`, `ClasificadorDesempenoTest`, `AutoevaluacionTest` |
| HU-05 Clasificación y acción institucional | Implementado | `ClasificadorDesempeno`, `GET /acciones-sugeridas`, `AccionesYPanelTest` |
| HU-06 Análisis de comentarios | Implementado | `RevisionHallazgosTest`, `AnalisisIaTest`, `CitasVerificablesTest` |
| HU-07 Retroalimentación y plan de mejoramiento | Vía acta | `ActaTest`, `PlanSeguimientoTest` |
| HU-08 Reportes | Vía I5 e I11 | `GET /kpis`, `GET /kpis/panel`, `AccionesYPanelTest` |
| HU-09 Evidencias complementarias | Futuro | No implementado |

ESC-09.2 se cumple sin HU-09: D12, sobre metodologías, herramientas y tecnologías, es un ítem
obligatorio de la autoevaluación y de la coevaluación.

## Reglas de la resolución

| Regla | Dónde se cumple |
|-------|-----------------|
| Anexo 1: 6 criterios y 14 descriptores (AUTO 14, HETERO 11, COEVAL 6) | V25, `FormulariosTest` |
| Ponderación 50/30/20 | V19, `PesosFuentes`, `GET /configuracion/evaluacion` |
| Escala de cinco categorías | `ClasificadorDesempeno` y sus pruebas de límites |
| Ciclo de semanas 11 a 16 | `TipoEtapa`, `GET /periodos-academicos/activo/calendario` |
| Seguimiento en el periodo siguiente | `PlanSeguimientoService`, `GET /docentes/{id}/plan-seguimiento` |
| Plan de Mejoramiento en categoría Bajo | `GeneradorPdfActa` |
| Muy bajo: la institución inicia el proceso según su normativa | `AccionSugeridaService`, solo informativo |

## Inconsistencias del Excel frente a la resolución

1. El Excel pone la coevaluación en "Proporciona retroalimentación"; el PDF la pone en "Desarrolla
   procesos evaluativos".
2. ESC-02.1 incluye "Acompañamiento" para coordinación y el PDF no.
3. El Excel reagrupa descriptores bajo el criterio "evaluar", y la celda combinada del PDF es
   ambigua.
4. HU-04 menciona "reglas de exención" que la resolución no define.
5. HU-05 habla de desvinculación automática; la norma solo dice que la institución inicia el
   proceso conforme a su normativa.
6. El Excel cita "Artículo 5 (Alcance)", pero el Art. 5 es la vigencia.

## Decisiones pendientes

Fuentes faltantes, umbral de 3,8 frente a 4,00 y regla de corrección de actas. Ver
[DECISIONES.md](DECISIONES.md).

## Criterios de aceptación de I5 e I11

**I5, indicadores generales**

- Muestra docentes evaluados, promedio general, destacados y calificaciones con su variación
  frente al periodo anterior.
- El coordinador ve solo su facultad; el vicerrector puede filtrar por facultad.
- Solo cuenta promedios COMPLETO. El docente recibe 403.

**I11, panel por programa**

- Por programa y en total muestra cobertura, promedio y distribución por categoría, en conteo y
  porcentaje.
- Se filtra por periodo y, para el vicerrector, por facultad. El coordinador ve lo suyo.
- No enlaza a expedientes ni identifica docentes. El docente recibe 403.
