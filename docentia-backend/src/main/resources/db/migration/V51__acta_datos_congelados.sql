-- El acta guarda una copia de todo lo que imprime. Así no cambia si después cambian el programa o la
-- escuela del docente, su nombre, el periodo, la cita, los pesos o el análisis de comentarios. La
-- copia se toma al crear el acta y se actualiza hasta la primera firma; desde ahí queda fija.
alter table actas
    add column docente_nombre         varchar(150),
    add column docente_correo         varchar(150),
    add column documento_rotulo       varchar(40),
    add column docente_documento      varchar(40),
    add column docente_cargo          varchar(300),
    add column programa_nombre        varchar(150),
    add column escuela_nombre         varchar(200),
    add column periodo_nombre         varchar(50),
    add column periodo_inicio         date,
    add column periodo_fin            date,
    add column evaluador_cargo        varchar(300),
    add column reunion_fecha          date,
    add column reunion_hora_inicio    time,
    add column reunion_hora_fin       time,
    add column reunion_asistencia     varchar(20),
    add column peso_estudiantes       numeric(5, 2),
    add column peso_coordinacion      numeric(5, 2),
    add column peso_autoevaluacion    numeric(5, 2),
    add column respuestas_estudiantes integer,
    add column fortalezas             text,
    add column aspectos_por_mejorar   text,
    add column formato_codigo         varchar(50),
    add column formato_version        varchar(20),
    add column facultad_id            bigint;

-- Las actas que ya existen toman los datos de hoy; las firmadas conservan su PDF y su hash, que
-- son la prueba de lo que se firmó.
with datos as (
    select a.id_acta, u.nombre_completo, u.correo, u.codigo, u.rol, pr.nombre as programa,
           'Escuela de ' || regexp_replace(trim(f.nombre), '^(facultad|escuela)\s+de\s+', '', 'i')
               as escuela,
           p.nombre as periodo, p.fecha_inicio, p.fecha_fin,
           pon.peso_estudiantes, pon.peso_coordinacion, pon.peso_autoevaluacion,
           coalesce(pr.id_facultad, u.id_facultad) as facultad_id
    from actas a
    join usuarios u on u.id_usuario = a.id_docente
    join periodos_academicos p on p.id_periodo = a.id_periodo
    left join programas pr on pr.id_programa = u.id_programa
    left join facultades f on f.id_facultad = coalesce(pr.id_facultad, u.id_facultad)
    left join ponderaciones pon on pon.id_periodo = a.id_periodo
)
update actas a set
    docente_nombre      = d.nombre_completo,
    docente_correo      = d.correo,
    documento_rotulo    = case when d.codigo is not null then 'Código' end,
    docente_documento   = d.codigo,
    programa_nombre     = d.programa,
    escuela_nombre      = d.escuela,
    docente_cargo       = case
        when d.programa is not null then 'Docente del programa ' || d.programa
        when d.rol = 'COORDINADOR' then
            'Docente y coordinador(a) de la ' || coalesce(d.escuela, 'No aplica')
        else 'Docente' end,
    evaluador_cargo     = case
        when d.rol = 'COORDINADOR' then 'Vicerrectoría Académica'
        else 'Coordinación de la ' || coalesce(d.escuela, 'No aplica') end,
    periodo_nombre      = d.periodo,
    periodo_inicio      = d.fecha_inicio,
    periodo_fin         = d.fecha_fin,
    peso_estudiantes    = coalesce(d.peso_estudiantes, 50),
    peso_coordinacion   = coalesce(d.peso_coordinacion, 30),
    peso_autoevaluacion = coalesce(d.peso_autoevaluacion, 20),
    facultad_id         = d.facultad_id
from datos d
where d.id_acta = a.id_acta;

update actas a set
    reunion_fecha       = fr.fecha,
    reunion_hora_inicio = fr.hora_inicio,
    reunion_hora_fin    = fr.hora_fin,
    reunion_asistencia  = c.asistencia
from citas c
join franjas_horarias fr on fr.id_franja = c.id_franja
where c.id_docente = a.id_docente
  and c.id_periodo = a.id_periodo
  and c.estado = 'ACTIVA';

-- Un acta firmada desde ahora lleva siempre la identificación, el periodo, la reunión y los pesos.
-- NOT VALID: las firmadas antes de esta versión no se revisan (su PDF ya tiene esos datos).
alter table actas add constraint ck_actas_firmada_completa check (
    estado <> 'FIRMADA' or (
        docente_nombre is not null and docente_correo is not null and docente_cargo is not null
        and evaluador_cargo is not null and periodo_nombre is not null
        and periodo_inicio is not null and periodo_fin is not null
        and reunion_fecha is not null and reunion_hora_inicio is not null
        and reunion_hora_fin is not null
        and peso_estudiantes is not null and peso_coordinacion is not null
        and peso_autoevaluacion is not null
    )
) not valid;
