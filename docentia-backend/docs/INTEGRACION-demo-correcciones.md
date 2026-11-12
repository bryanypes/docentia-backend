# Integración de `demo/correcciones-12nov` con `develop`

Solo plan: no se ha ejecutado. La rama sale de `release/0.4.0`; `develop` avanzó con el PR #12
(`feature/admin-refactor`) y el PR #24 (rutas públicas exactas bajo `/sesiones`). Una simulación con
`git merge-tree` da estos 18 archivos en conflicto.

## Migraciones (lo primero)

| develop | rama | Resolución |
|---------|------|------------|
| `V42__agregar_rol_administrador.sql` (agrega ADMINISTRADOR al check de rol y de alcance) | `V42__recuperacion_de_contrasena.sql` | **Se deja la V42 de develop tal cual.** La recuperación de contraseña pasa a **V43** sin cambiar su contenido |
| — | `V43__roles_administrador_y_visualizador.sql` | Pasa a **V44** y solo agrega VISUALIZADOR: los checks deben incluir `ADMINISTRADOR` (ya puesto por la V42 de develop) y `VISUALIZADOR`. Con alcance nulo para los dos |

Comprobar con una base nueva (`docker compose down -v`) y con una copia de una base en V42 de develop.

## Rol ADMINISTRADOR: una sola definición

develop (PR #12) y la rama definen ADMINISTRADOR distinto. Se toma esta definición única (la de
`docs/ROLES.md` de la rama), revisando las anotaciones del PR #12 contra ella:

- Administración: usuarios (escritura), facultades y programas (escritura), ponderaciones (escritura),
  periodos y fechas, uso de IA. Sin docentes, notas, comentarios, análisis ni actas.
- Vicerrectoría: lectura de usuarios, catálogos y ponderaciones; decide fechas; paneles, análisis,
  auditoría y corrección de actas.
- Visualización: `GET /kpis`, `GET /kpis/panel` y lectura de catálogos.

## Archivo por archivo

| Archivo | Cómo resolver |
|---------|---------------|
| `entity/Rol.java` | Unir: los seis valores una sola vez |
| `entity/Usuario.java` | Un solo `case VICERRECTOR, ADMINISTRADOR, VISUALIZADOR` sin vínculo; quitar `crearAdministrador` duplicado si sobra |
| `config/SecurityConfig.java` | Tomar develop (rutas exactas del #24) y **agregar** las dos de recuperación: `POST /sesiones/recuperacion` y `/sesiones/recuperacion/restablecimiento`. Nunca `/sesiones/**` |
| `config/CargadorDatosDesarrollo.java` | Un solo usuario demo por rol: `administrador.demo` y `visualizador.demo` de la rama; quitar el `admin@` del #12 si queda duplicado |
| `controller/FacultadController.java`, `ProgramaController.java` | Lectura: V, C, A, L. Escritura: solo A |
| `controller/PeriodoAcademicoController.java` | Crear, editar y activar: V y A |
| `controller/KpiController.java` | C, V y L |
| `security/UsuarioSesion.java` | Conservar `veCatalogoDeFacultad` de la rama; revisar el alcance global que el #12 dio al administrador (no debe alcanzar docentes) |
| `test/.../MatrizAutorizacionTest.java` | Tomar la de la rama (seis roles, rutas públicas cerradas, recuperación) y añadir las rutas que el #12 haya creado |
| `test/.../CuentaUsuarioTest.java`, `UsuarioControllerTest.java`, `PonderacionControllerTest.java` | Escrituras con `comoAdministrador()`; unificar con los ajustes del #12 |
| `DocenteIA/src/shared/constants/dominio.js` | ROLES y ETIQUETA_ROL con los seis; quitar duplicados |
| `DocenteIA/src/shared/components/layout/Sidebar/Sidebar.jsx` | Menús de la rama (sin Matrícula, Auditoría, Facultades ni carga CSV; administración y visualización propios) |
| `DocenteIA/src/app/AppRouter.jsx` | Rutas de la rama: sin `/importaciones` ni `/matricula`; `/panel-programas` para C, V y L; recuperación de contraseña pública |
| `DocenteIA/src/features/docentes/pages/Docentes.jsx` | Unir los filtros de la rama (clasificación y orden) con los cambios del #12 |
| `PENDIENTES-ENTREGA.md`, `docs/sustentacion/GUION_PRUEBA_Y_DEMO.md` | Conservar ambas secciones; el guion de la rama manda en credenciales y flujos |

## Orden recomendado

1. Rama `integracion/correcciones-12nov` desde `develop`.
2. Migraciones (renumerar V43 y V44) y `Rol`/`Usuario`. Compilar.
3. Seguridad y controladores. Correr `MatrizAutorizacionTest`, `CuentaUsuarioTest`,
   `UsuarioControllerTest`, `RecuperacionContrasenaTest`, `PrimerUsuarioTest`.
4. Frontend: dominio, menú, rutas, Docentes. `npx vitest run`, `npx oxlint`, `npx vite build`.
5. Documentación.
6. Suite completa (`./mvnw verify`) y E2E con semilla nueva. PR a develop con CI verde.
