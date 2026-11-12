# Integración de los PR #8 y #12 en develop

Este documento dice qué quedó en `develop` de los PR #8 y #12 y qué no, y por qué.

- **PR #8** (`feature/V1-vistas-paneles-y-comentarios`, vXro1): vistas por rol, análisis de comentarios y proceso de evaluación.
- **PR #12** (`feature/admin-refactor`, Daniel Aranda): rol ADMINISTRADOR y cambio de términos de "fuentes" a "evaluadores".

Las ramas originales siguen en GitHub sin cambios. El `develop` anterior a la reescritura está en `backup/develop-pre-force`.

## Decisión

`develop` conserva la versión final que se probó en la presentación. Las funciones del PR #8 que no están (tipo c) **no se portaron**: quedan como opcionales y las decide el dueño del proyecto.

## Autoría

- **PR #8:** sus 60 commits ya están en el historial de `develop`, a nombre de vXro1 (y de Bryan Yepes en los commits del análisis de comentarios).
- **PR #12:** sus 21 commits se reaplicaron en `develop` con su autor (Daniel Aranda) y su fecha originales.
  - Ninguno de esos commits cambia el código: su función ya está cubierta en `develop`, se retiró a propósito o se rehízo aparte (ver la tabla).
  - Cada mensaje lo indica y remite a este documento.
  - Se quitaron las líneas `Co-authored-by` de herramientas de IA.

## Paridad

Tipos:
- **(a) reemplazado:** hay un equivalente en `develop`.
- **(b) retirado o descartado a propósito.**
- **(c) no existe hoy:** opcional, pendiente de decisión.

### PR #8

| Lo que falta | Tipo | Dónde quedó o por qué |
| --- | --- | --- |
| Migraciones V29 a V33 (trazabilidad, firma dibujada, nombres con mayúscula, tildes, cita gestionada por coordinación) | a | Las mismas migraciones, renumeradas como V36 a V40 |
| `PanelesUniversidad` (embudo del proceso y panorama por facultad) | a | Paneles "Avance del proceso" y "Comparación por facultad / programa" en Estadísticas |
| `RegistroAuditoria` (Usuarios → Actividad) y su prueba `Usuarios.test.jsx` | a | Página `/auditoria` |
| `Portada` (franja de inicio) | a | Inicio rediseñado por rol |
| `AnalisisDocente` (análisis de un docente en la página Análisis) | a | Panorama del análisis y DocenteIA en `/analisis` |
| `Facultades.css` y `Facultades.test.jsx` (tarjetas de facultad) | a | Página Facultades y Programas de administración |
| `Coevaluaciones.css` y `EvaluacionProfesores.css` | a | Estilos de los formularios por pasos (`AsistentePasos`) |
| `SemillaComentarios` y `SemillaFormularios` (datos de desarrollo) | a | `CargadorDatosDesarrollo` y `SemillaHeteroevaluacion` |
| `funciones.js` (interruptores para ocultar funciones) | b | Lo que ocultaba (CSV, matrícula y cobertura) se retiró del menú |
| Matrícula y cobertura por CSV | b | Retirado a propósito; la ruta `/matricula` ya no está |
| Hojas de embudo, facultades y temas en el reporte Excel, y su prueba | b | Descartado por decisión del dueño; el Excel se queda como está |
| Resumen del análisis de comentarios en el expediente (`ResumenAnalisisIA`, `ComentariosDocente`, `ResultadoComentarios`) | c | Opcional, pendiente de decisión. El backend (`/analisis-comentarios/docentes/{id}`) sigue disponible |
| Temas más mencionados en Estadísticas (`ComentariosResumen`) | c | Opcional, pendiente de decisión. El backend (`/analisis-comentarios/resumen`) sigue disponible |
| Celebración y resumen local del estudiante al terminar (`CelebracionFinal`, `ResumenEstudiante`, `resumenLocal`) | c | Opcional, pendiente de decisión |
| Búsqueda de una palabra en los comentarios del panorama (`BusquedaComentariosService`, `CoincidenciaComentariosResponse`) | c | Opcional, pendiente de decisión |
| Comentario general en los formularios (V35 `comentario_general_formulario`) | c | Opcional, pendiente de decisión |

### PR #12

| Lo que falta | Tipo | Dónde quedó o por qué |
| --- | --- | --- |
| V42 `agregar_rol_administrador` | a | V43 `roles_administrador_y_visualizador`. Su V42 chocaba en número con la de recuperación de contraseña |
| Permisos de ADMINISTRADOR en controladores, rutas y menú | a | Roles redefinidos en `develop`: ADMINISTRADOR gestiona usuarios, catálogos, ponderaciones y fechas; VICERRECTOR consulta y activa periodos |
| Ocultar crear, editar y eliminar a VICERRECTOR | a | Cubierto por la misma redefinición de roles |
| Bloquear el CSV de importación mientras HU-01 esté activa | b | El CSV se retiró |
| Abrir `/sesiones/**` sin autenticación | b | Descartado por seguridad (PR #24): solo `POST /sesiones` y `POST /sesiones/google` son públicos, más los dos de recuperación de contraseña |
| Cambiar "Fuentes" por "Evaluadores" en la interfaz | a | Incluido en 0.4.0, solo en los textos visibles. Los campos de la API (`fuentesPendientes`) se conservan: el PR los renombraba solo en el frontend y el backend seguía enviando el nombre original |

## Funciones anunciadas, comprobadas en pantalla sobre develop

| Función | ¿Funciona en develop? |
| --- | --- |
| Estadísticas en una sola vista, con filtros que recalculan todo | Sí |
| Reporte Excel | Sí |
| Trazabilidad del proceso | Sí |
| Firma de actas (dibujada, escrita o PNG subido) | Sí |
| Citas: confirmar, registrar asistencia y reprogramar | Sí |
| Formularios por pasos | Sí |
| Rol ADMINISTRADOR | Sí |
| Análisis de comentarios por docente en el expediente | No (tipo c, opcional) |
