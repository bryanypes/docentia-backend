# DOCENTIA — backend

Sistema de evaluación docente de la Corporación Universitaria Autónoma del Cauca.

Este repositorio es la API REST. La aplicación web está en
[docentia-frontend](https://github.com/bryanypes/docentia-frontend).

Stack: Spring Boot, Java 21, PostgreSQL, Flyway y Maven.

## Cómo correrlo en local

Requisitos: JDK 21, Maven y PostgreSQL 14 o superior.

`JAVA_HOME` tiene que apuntar a un **JDK 21**, no a un JRE ni a otra versión. Con `mvn -v` se
comprueba qué Java usa Maven; si no es 21, la compilación falla.

1. Crear la base de datos:

   ```sql
   create role docentia login password 'docentia';
   create database docentia owner docentia;
   ```

   Si la base, el usuario o el puerto son otros, se cambian con las variables
   `DOCENTIA_DB_URL`, `DOCENTIA_DB_USUARIO` y `DOCENTIA_DB_CONTRASENA`.

2. Arrancar la API (queda en `http://localhost:8080/api`). Flyway crea las tablas al arrancar:

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Usuarios de desarrollo

El login con correo y contraseña es temporal: existe solo mientras se integra el SSO
institucional. Con el perfil `dev`, y si la tabla de usuarios está vacía, se crean estos
usuarios con la contraseña `Docentia2026*`:

| Rol          | Correo                                     | Alcance                |
| ------------ | ------------------------------------------ | ---------------------- |
| Vicerrector  | vicerrectoria@uniautonoma.edu.co           | Toda la universidad    |
| Coordinador  | coordinacion.ingenieria@uniautonoma.edu.co | Facultad de Ingeniería |
| Coordinador  | coordinacion.derecho@uniautonoma.edu.co    | Facultad de Derecho    |
| Docente      | julian.hoyos@uniautonoma.edu.co            | Ingeniería de Software |
| Docente      | luisa.mosquera@uniautonoma.edu.co          | Derecho                |

Además quedan el periodo 2026-1 cerrado y el 2026-2 activo. En 2026-2, Julián Hoyos y Diana
Astaiza tienen las tres fuentes (estudiantes, coordinación y vicerrectoría), así que su promedio
sale completo; los demás docentes siguen sin calificar y su promedio sale incompleto.

Las etapas de 2026-2 son: recolección de calificaciones del 28 de septiembre al 13 de noviembre,
revisión de hallazgos del 13 de octubre al 20 de noviembre, agenda de citas del 13 de octubre al 27 de
noviembre y firma de actas del 30 de noviembre al 4 de diciembre. **En desarrollo las etapas se
solapan a propósito** para poder demostrar todo el flujo antes de la entrega del 6 de noviembre; en
producción las define la vicerrectoría. Cada acción solo se puede hacer dentro de su etapa; fuera de
ella la API responde 409. Si hace falta probar otra fecha, se cambian las etapas desde la pantalla de
periodos.

En 2026-2 también hay comentarios de estudiantes, como si vinieran del CSV: Julián tiene 13 (dos
son ofensivos o sobre su persona, para probar el filtro) y Diana 6, así que su análisis con IA sale
"evidencia insuficiente".

Para las citas de retroalimentación, el coordinador de Ingeniería tiene franjas abiertas el 9 de
noviembre (8:00 a 10:00) y el 10 de noviembre (14:00 a 15:00), y la coordinadora de Derecho una el 9
de noviembre a las 10:00.

Fuera del perfil `dev` hay que definir `DOCENTIA_JWT_SECRETO` (mínimo 32 caracteres).

### Recrear la base de desarrollo

Los datos de ejemplo **solo se cargan si la tabla `usuarios` está vacía**. Si la base ya tiene
datos de una versión anterior, los cambios del seed no aparecen hasta recrearla:

```sql
-- con la API detenida, desde psql como superusuario
drop database docentia;
create database docentia owner docentia;
```

Al arrancar de nuevo con el perfil `dev`, Flyway crea las tablas y se cargan los datos de ejemplo.

Los promedios ponderados se guardan en la tabla `promedios_docente` y se recalculan cada vez que
cambia una calificación o la ponderación del periodo. Si una base trae calificaciones sin promedio
guardado, se calculan solos al arrancar la API.

### Análisis de comentarios con IA

El análisis de los comentarios de estudiantes (`/api/analisis-ia`) usa un modelo de lenguaje.

- Con el perfil `dev` corre un **modelo simulado** que no usa la red y responde siempre lo mismo.
- Fuera de `dev` el análisis está **apagado** (`DOCENTIA_IA_HABILITADO=false`).

> **Antes de habilitarlo con un proveedor externo** hay que revisar la política de tratamiento de
> datos personales de la universidad (Ley 1581 de 2012): los comentarios de los estudiantes salen
> del sistema hacia un tercero. El backend quita correos, códigos, números largos y nombres cuando
> el estudiante se presenta, y no envía el nombre del docente, pero ese filtro no es perfecto.

| Variable                       | Para qué                                                         |
| ------------------------------ | ---------------------------------------------------------------- |
| `DOCENTIA_IA_HABILITADO`       | `true` para aceptar solicitudes                                  |
| `DOCENTIA_IA_PROVEEDOR`        | `simulado` o `chat-completions` (API con formato chat/completions) |
| `DOCENTIA_IA_URL_BASE`         | dirección del proveedor, por ejemplo `https://…/v1`              |
| `DOCENTIA_IA_MODELO`           | identificador del modelo en el proveedor                         |
| `DOCENTIA_IA_CLAVE`            | clave del proveedor (nunca en archivos del repositorio)          |
| `DOCENTIA_IA_TIMEOUT_SEGUNDOS` | espera máxima por respuesta (60 por defecto)                     |

El texto de instrucción está versionado en `src/main/resources/prompts/`; cada borrador guarda el
modelo, la versión de la instrucción y la fecha.

## Pruebas y formato

```bash
mvn test            # usa un PostgreSQL embebido, no necesita la base local
mvn spotless:apply  # Google Java Format
```

## Ramas

Se trabaja con GitFlow: `main` y `develop` son permanentes y cada historia de usuario
sale de `develop` en una rama `feature/{id}-{descripcion-corta}`.

Los commits siguen el formato `feat:`, `fix:`, `refactor:` o `style:` con la descripción en español.
