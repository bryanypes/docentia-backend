-- Consultas de verificación de DOCENTIA. Solo lectura: todo corre dentro de una transacción READ ONLY
-- que se revierte al final. Sirven antes y después del despliegue: usan solo columnas que ya existían
-- en la versión 0.3.3 (migración V28).
--
-- Uso, desde la carpeta del docker compose del servidor:
--   docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < docentia-backend/docs/consultas-verificacion.sql
-- Guarda la salida de antes y la de después del despliegue para compararlas (sección 7).

begin transaction read only;

-- 1. Versión del esquema. Antes del despliegue de 0.4.0 debe terminar en 28; después, en 41.
--    Si aparece alguna de V29 a V33, la base aplicó migraciones de una rama que no está en develop:
--    no despliegues y revisa (ver OPERACION.md, ensayo de migraciones).
select version, description, installed_on, success
from flyway_schema_history
order by installed_rank desc
limit 12;

select count(*) as migraciones_v29_a_v33
from flyway_schema_history
where version in ('29', '30', '31', '32', '33');

select count(*) as migraciones_fallidas
from flyway_schema_history
where not success;

-- 2. Periodo activo y ventanas de sus etapas.
select p.nombre, p.fecha_inicio, p.fecha_fin, e.tipo_etapa, e.fecha_apertura, e.fecha_cierre,
       (now() at time zone 'America/Bogota')::date between e.fecha_apertura and e.fecha_cierre
           as abierta_hoy
from etapas_periodo e
join periodos_academicos p on p.id_periodo = e.id_periodo
where p.es_activo
order by e.fecha_apertura, e.tipo_etapa;

-- 3. Docentes activos con menos de 5 respuestas del formulario de estudiantes en el periodo activo, y
--    si tienen nota de estudiantes por otra vía (CSV). Sin ninguna de las dos, su promedio no queda
--    COMPLETO y su acta no se puede firmar (DECISIONES.md, P4).
select u.nombre_completo,
       u.correo,
       count(r.id) as respuestas_formulario,
       exists (
           select 1 from calificaciones c
           where c.id_docente = u.id_usuario
             and c.id_periodo = (select id_periodo from periodos_academicos where es_activo)
             and c.fuente = 'ESTUDIANTES'
       ) as tiene_nota_estudiantes
from usuarios u
left join respuestas_heteroevaluacion r
       on r.docente_id = u.id_usuario
      and r.periodo_id = (select id_periodo from periodos_academicos where es_activo)
where u.rol = 'DOCENTE' and u.es_activo
group by u.id_usuario, u.nombre_completo, u.correo
having count(r.id) < 5
order by respuestas_formulario, u.nombre_completo;

-- 4. Promedios del periodo activo por estado (COMPLETO, PARCIAL, SIN_NOTA).
select estado, count(*) as docentes
from promedios_docente
where id_periodo = (select id_periodo from periodos_academicos where es_activo)
group by estado
order by estado;

-- 5. Usuarios de la semilla de desarrollo. En producción debe dar 0: si no, la base se creó con el
--    perfil dev y esas cuentas tienen una contraseña pública.
select count(*) as usuarios_de_demo
from usuarios
where lower(correo) in (
    'vicerrectoria@uniautonoma.edu.co',
    'coordinacion.ingenieria@uniautonoma.edu.co',
    'coordinacion.derecho@uniautonoma.edu.co',
    'coordinacion.finanzas@uniautonoma.edu.co',
    'julian.hoyos@uniautonoma.edu.co',
    'diana.astaiza@uniautonoma.edu.co',
    'nora.munoz@uniautonoma.edu.co',
    'camilo.vidal@uniautonoma.edu.co',
    'ramiro.paz@uniautonoma.edu.co',
    'laura.demo@uniautonoma.edu.co'
) or lower(correo) like 'estudiante.demo%@uniautonoma.edu.co';

-- 6. Trabajo pendiente en segundo plano: correos sin enviar o con error y análisis en curso.
select 'correo ' || estado as cola, count(*) as filas
from cola_correo
group by estado
union all
select 'analisis ' || estado, count(*)
from solicitudes_analisis
group by estado
order by cola;

-- 7. Conteos para comparar antes y después del despliegue. Deben coincidir: las migraciones V34 a V41
--    agregan tablas y columnas, no borran filas.
select 'usuarios' as tabla, count(*) as filas from usuarios
union all select 'calificaciones', count(*) from calificaciones
union all select 'promedios_docente', count(*) from promedios_docente
union all select 'participaciones_heteroevaluacion', count(*) from participaciones_heteroevaluacion
union all select 'respuestas_heteroevaluacion', count(*) from respuestas_heteroevaluacion
union all select 'evaluaciones_formulario', count(*) from evaluaciones_formulario
union all select 'matriculas', count(*) from matriculas
union all select 'citas', count(*) from citas
union all select 'actas', count(*) from actas
union all select 'auditoria', count(*) from auditoria
order by tabla;

rollback;
