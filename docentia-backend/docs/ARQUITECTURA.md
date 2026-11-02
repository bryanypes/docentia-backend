# Arquitectura

```mermaid
flowchart TB
  N[Navegador: React y Vite] --> G[nginx]
  G -- /api --> C[Controladores REST]
  C --> S[Servicios de negocio]
  S --> R[Repositorios JPA y SQL]
  R --> DB[(PostgreSQL 16)]
  S -- evento tras el commit --> NO[Notificaciones]
  NO --> DB
  W[Procesador de análisis] -- SKIP LOCKED --> DB
  W --> IA[Modelo simulado o proveedor externo]
  M[Procesador de correo] -- SKIP LOCKED --> DB
  M --> SMTP[SMTP o Mailpit]
```

## Modelo del instrumento

```mermaid
erDiagram
  CRITERIOS_EVALUACION ||--o{ DESCRIPTORES_EVALUACION : agrupa
  DESCRIPTORES_EVALUACION ||--o{ DESCRIPTOR_ACTOR : "lo responde"
  EVALUACIONES_FORMULARIO ||--o{ RESPUESTAS_DESCRIPTOR : contiene
  EVALUACIONES_FORMULARIO ||--o{ COMENTARIOS_CRITERIO : contiene
  EVALUACIONES_FORMULARIO }o--|| CALIFICACIONES : "al enviar alimenta"
  CALIFICACIONES }o--|| PROMEDIOS_DOCENTE : recalcula
```

El catálogo es dato de migración, marcado con `RES074-2026-ANEXO1`. Al enviar un formulario, la media
de sus descriptores se vuelve la nota de la fuente en `calificaciones` y corre el mismo recálculo que
cualquier otra nota: pesos, categoría, avisos y actas no cambian.

## Anonimato de la heteroevaluación

```mermaid
flowchart LR
  E[Estudiante] -- envía --> S[HeteroevaluacionService]
  S -- quién ya respondió --> P[(participaciones_heteroevaluacion<br/>asignación, estudiante, fecha)]
  S -- qué respondió --> R[(respuestas_heteroevaluacion<br/>UUID, asignación, docente, periodo)]
  R --> V[(respuestas_descriptor_hetero)]
  R -- media con 5 o más respuestas --> C[(calificaciones: ESTUDIANTES)]
  P -- conteos --> K[Cobertura]
```

Las dos tablas se escriben en la misma transacción, pero ninguna columna las une. La participación solo
impide el doble envío y mide la cobertura; la respuesta solo alimenta la nota del docente cuando hay al
menos 5.

## Decisiones importantes

**Capas.** Los controladores solo traducen HTTP, los servicios tienen las reglas y los repositorios el
acceso a datos. Las reglas se prueban con PostgreSQL real.

**Alcance en el servicio.** El rol se valida en el método y el alcance por facultad en
`DocenteService.buscarDocenteVisible`. Un recurso fuera de alcance responde 404, igual que uno
inexistente, para no confirmar que existe.

**A4.** El docente nunca recibe notas por fuente ni un promedio parcial. La regla está en los DTO que
se le devuelven, no en la pantalla.

**Análisis asíncrono.** La solicitud se guarda en EN_COLA y responde 202. Un procesador la toma con
`FOR UPDATE SKIP LOCKED`, llama al modelo fuera de transacción y guarda el borrador. Al arrancar, lo
huérfano vuelve a la cola o queda en ERROR tras tres intentos.

**Outbox de notificaciones.** El negocio publica un evento y el aviso se crea después del commit. El
correo se escribe en una tabla y otro proceso lo envía. Un SMTP caído no afecta una firma. Ver
[NOTIFICACIONES.md](NOTIFICACIONES.md).

**Ventanas de etapa.** Cada etapa tiene fechas y las operaciones consultan `VentanaEtapaService` con un
`Clock`. No hay tareas que abran o cierren nada: la fecha decide, y una etapa cerrada responde 409.

**Actas inmutables.** Un trigger de la base rechaza cambios en un acta firmada. La corrección crea una
versión nueva y anula la anterior.

**Concurrencia.** `@Version` protege actas, hallazgos y citas. La reserva bloquea la franja y la
publicación bloquea la solicitud. Los índices únicos son la última defensa.

**Auditoría.** Se inserta en su propia transacción para conservar los intentos fallidos, y un trigger
rechaza cambios y borrados.

**Errores.** Todo error sale en RFC 7807 con `correlationId`. Ese mismo identificador va en los logs y
en la cabecera `X-Correlation-Id`.
