# DOCENTIA: resumen ejecutivo

DOCENTIA es el sistema de evaluación docente de la Corporación Universitaria Autónoma del Cauca. Lleva
el proceso que define la Resolución 074 de 2026: desde que estudiantes, coordinación y docente responden
sus instrumentos hasta que el docente firma su acta y se hace seguimiento a sus compromisos.

## Qué hace

1. **Recoge las tres evaluaciones** con los instrumentos del Anexo 1: heteroevaluación de estudiantes
   (11 ítems), coevaluación de coordinación (6) y autoevaluación del docente (14), en escala de 1 a 5.
   Los estudiantes responden en la aplicación y de forma anónima; también se acepta un CSV de respaldo.
2. **Calcula la nota** ponderada 50 % estudiantes, 30 % coordinación y 20 % autoevaluación, y la
   clasifica en cinco categorías con su acción institucional: muy alto (desde 4,60), alto (3,80),
   aceptable (3,00), bajo (2,20) y muy bajo.
3. **Analiza los comentarios** de los estudiantes y propone fortalezas y oportunidades de mejora, cada
   una con citas textuales verificables. Por defecto lo hace un modelo local por reglas, sin enviar nada
   fuera de la universidad. Coordinación valida, edita o descarta cada hallazgo antes de publicarlo.
4. **Cierra el ciclo con el docente.** Coordinación abre horarios, el docente reserva su cita y, en la
   reunión, ambos firman el acta (de retroalimentación o de compromiso). El PDF lleva la firma
   dibujada, el hash del contenido y un código de verificación público; el logo institucional se
   configura en el servidor (el archivo oficial no está en el repositorio). Bajo 3,80 el acta exige compromisos con plazo, que se
   siguen en el periodo siguiente.
5. **Informa a la dirección:** indicadores, estadísticas exportables a Excel, panel por programa sin
   docentes identificables, acciones sugeridas por categoría y auditoría de cada acción sensible.

Cada etapa (recolección, revisión de hallazgos, agenda de citas y firma de actas) abre y cierra por las
fechas que fija vicerrectoría; fuera de su ventana, la acción se rechaza.

## Qué cubre de la Resolución 074

| Historia | Estado |
|----------|--------|
| HU-01 Formulario de estudiantes | Hecho, anónimo, con mínimo de 5 respuestas por docente |
| HU-02 Coevaluación y HU-03 Autoevaluación | Hechas |
| HU-04 Nota final y HU-05 Clasificación y acción | Hechas |
| HU-06 Análisis de comentarios | Hecho, modelo local por defecto y modelo de lenguaje opcional |
| HU-07 Retroalimentación y plan de mejoramiento | Hecho, vía acta y seguimiento de compromisos |
| HU-08 Reportes | Hecho, vía indicadores, estadísticas y panel por programa |
| HU-09 Evidencias complementarias | No implementado |

## Lo que quedó fuera o depende de otros

- **HU-09** (evidencias complementarias y grupos focales).
- **Integración en vivo con Campus/NOVA:** no hay conexión directa; la inscripción de estudiantes en sus cursos usa el servicio de matrícula del backend (sin pantalla) mientras se define esa integración (por confirmar).
- **Ingreso de estudiantes:** fuera de desarrollo solo entran con Google institucional, que necesita un
  cliente OAuth de la universidad (aún sin configurar).
- **Decisiones pendientes de Vicerrectoría:** umbral de compromisos (3,8 aplicado frente a 4,00 del
  backlog), escala de 1 a 5, qué hacer con docentes con menos de 5 respuestas de estudiantes (hoy su
  acta no se puede firmar sin CSV de respaldo) y quién corrige un acta.
- **El modelo de lenguaje externo** está desactivado por defecto. Con datos reales exige revisar la
  política de tratamiento de datos (Ley 1581 de 2012). Las métricas del modelo local se midieron con
  comentarios sintéticos, no con comentarios reales.

## En números

Backend en Spring Boot 3.5 con Java 21 y PostgreSQL 16, con 36 migraciones; frontend en React 19 con
Vite. 1233 pruebas de backend, 121 de frontend y 11 de punta a punta con navegador real. Todo se levanta
con `docker compose up --build`.
