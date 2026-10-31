# Decisiones

Decisiones tomadas donde la Resolución 074 de 2026, el Excel de historias o el código dejaban
ambigüedad. La fuente de verdad es la resolución; el Excel v2 tiene errores (ver
[RES074-TRAZABILIDAD.md](RES074-TRAZABILIDAD.md)).

## Pendientes de Vicerrectoría

| # | Tema | Decisión aplicada | Quién la confirma |
|---|------|-------------------|-------------------|
| P1 | Fuentes faltantes (Art. 3) | Se renormaliza entre las fuentes disponibles y el promedio queda PARCIAL con las faltantes expuestas. Sin ninguna fuente queda SIN_NOTA, nunca 0. | Vicerrectoría |
| P2 | Umbral de compromisos | 3,8 por defecto, que es el inicio de la categoría Alto. El requerimiento original decía 4,00. Es configurable con `DOCENTIA_COMPROMISOS_UMBRAL`. | Vicerrectoría |
| P3 | Quién corrige un acta firmada | Solo Vicerrectoría, con motivo. Configurable con `DOCENTIA_ACTAS_ROL_CORRECTOR`. | Vicerrectoría |

## Motor

- **Migración.** La instrucción pedía V18, pero V18 ya existía. Se usan V19 a V21 y no se editó
  ninguna migración aplicada.
- **La autoevaluación reemplaza a la vicerrectoría como tercera fuente.** La Res. 074 pondera
  estudiantes 50 %, coordinación 30 % y docente 20 %. Se retiraron los endpoints
  `/calificaciones/vicerrectoria` y la fuente pasa a `AUTOEVALUACION`.
- **Redondeo.** A dos decimales HALF_UP antes de clasificar.
- **Categoría en PARCIAL.** Coordinación y Vicerrectoría ven la categoría provisional, marcada
  como parcial. El docente no ve valor ni categoría hasta que el promedio esté COMPLETO (A4).
- **Agregados.** I11 y los indicadores cuentan solo promedios COMPLETO.
- **Seed.** Se agregaron tres docentes (2,85, 4,75 y 1,90), por lo que los indicadores de la
  demo cambian frente a los del Sprint 3.

## Actas

- **Firma.** Exige acta en borrador, confirmación explícita, etapa FIRMA abierta, cita activa y
  promedio COMPLETO. Firmar con un promedio PARCIAL se rechaza con 409, porque el acta es el
  resultado definitivo.
- **Documento del firmante.** Los usuarios no tienen número de documento; el PDF usa el correo
  institucional como identificación.
- **Dos hashes.** El PDF imprime el SHA-256 del contenido canónico. El SHA-256 del archivo se guarda
  aparte, porque un archivo no puede contener su propio hash.
- **PDF sin notas por fuente.** El docente lo descarga, así que solo lleva el resultado final (A4).
- **Verificación pública.** Devuelve validez y datos del acta, no notas.
- **Acción Muy bajo.** El sistema solo muestra "Alerta informativa para Vicerrectoría — la
  institución evalúa el proceso conforme a su normativa". No envía correos, no bloquea ni cambia
  nada, y el texto no menciona desvinculación.

## Análisis de comentarios (E4)

- **Publicación explícita.** Incluir o descartar no le muestra nada al docente. Hay que publicar,
  sin hallazgos por decidir y con al menos uno incluido. Después la revisión no cambia.
- **Editar no toca las citas.** Se corrige la redacción; las citas siguen siendo las verificadas.
- **Versión GENERADO.** Se crea en el primer cambio del hallazgo, con el modelo y la instrucción.
- **El docente ve** tipo y texto. No ve citas ni comentarios, para no exponer a quien escribió.

## Calendario

- **Las ventanas siguen siendo por fecha, sin scheduler,** y responden 409. La semana de la
  resolución es una etiqueta informativa.
- **Etapa vigente.** Si varias ventanas se solapan, como en el seed, se toma la más avanzada.
- **Última franja del día.** 23:00–23:30. La de 23:30–00:00 cruza de día y antes terminaba en un
  409 genérico; ahora recibe un mensaje claro.

## Notificaciones

- **Textos fijos por tipo.** La privacidad queda garantizada por diseño y no por revisar cada envío.
- **Alerta Muy bajo.** Solo con promedio COMPLETO, para no alarmar con un resultado provisional. El
  texto no nombra al docente ni la categoría, porque el correo no puede llevarla.
- **Cancelación de citas.** No hay endpoint para cancelar (F3 no se implementó) y este alcance no
  agrega funciones. Se cubre el cambio de cita, que libera la anterior.
- **Acta lista para firma.** Se avisa cuando se puede firmar: al crearla, o al agregar el compromiso
  si la nota lo exige.
- **Compromisos de actas en borrador.** No generan recordatorios hasta que el acta esté firmada.
- **Columnas de V22.** Siguen los nombres pedidos (`usuario_id`), no la convención `id_usuario`.

## Publicación

- **Gate de autores.** `git shortlog` muestra a Daniel Aranda y a Vero, colaboradores reales con
  commits ya públicos. Quitarlos exigiría reescribir historial, y eso está prohibido. Tus dos correos
  se unifican con `.mailmap`.
- **Repositorio `docentia-docker`.** Su commit local quedó absorbido por el monorepo y no se sube.
- **PostgreSQL en CI.** Las pruebas usan `embedded-postgres`, un PostgreSQL 16 real, así que no hace
  falta un servicio aparte.

## Endurecimiento

- **Numeración.** V22 notificaciones, V23 auditoría y V24 bloqueo optimista con índices.
- **Campo de bloqueo.** Se llama `version_bloqueo`, porque `version` ya es la versión del acta.
- **Promedios.** Se escriben con un upsert atómico; `version_bloqueo` sube en cada recálculo.
- **Rechazo por validación.** Bean Validation corre antes que `@PreAuthorize`: un rol sin permiso con un
  cuerpo inválido recibe 400 o 415 en lugar de 403. No expone datos. La matriz acepta los tres.
- **RFC 7807 compatible.** Se conservan `mensaje` y `errores`, que lee el frontend.
- **Tope de listados.** La respuesta se corta en 1000 filas de forma central, sin romper el contrato. Es
  un tope de respuesta, no paginación en base de datos.
- **429 en el login.** Se cuenta igual si el correo no existe, por eso no revela usuarios.
- **Correlación.** El identificador lo genera el servidor; no se acepta del cliente.
- **Exportación CSV.** No existe, así que la protección contra fórmulas no aplica.
- **Swagger.** No está instalado; la API se documenta en `docs/` y Postman.
- **Zona horaria.** La API corre en `America/Bogota`, igual que su reloj. La cola de correo compara con
  el reloj de la base, para no depender de que ambos coincidan.
- **Campana.** Va fija en el contenedor principal para que las páginas no dependan de ella.

## Estructura de repositorios

- El backend se trabaja en `docentia-backend`, carpeta `docentia-backend/`.
- El frontend se trabaja en el repositorio `DocenteIA`. La carpeta `DocenteIA/` del monorepo es una
  copia para Docker y se sincroniza al cerrar la versión. El login con Google solo estaba en esa
  copia y se portó a `DocenteIA`.
- La instrucción hablaba de 82 pruebas de frontend existentes; en `DocenteIA` había 26. Todas
  siguen en la suite.
