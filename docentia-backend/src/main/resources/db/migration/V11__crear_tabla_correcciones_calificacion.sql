-- Cada vez que una calificación ya registrada cambia de notas queda una fila aquí, con la nota de la
-- fuente (promedio de sus cinco aspectos) y el promedio ponderado del docente antes y después.
-- Cuando existan actas firmadas, el acta copiará el promedio de promedios_docente en ese momento;
-- las correcciones posteriores quedan en esta tabla pero no tocan el acta.
create table correcciones_calificacion (
    id_correccion            bigint generated always as identity primary key,
    id_calificacion          bigint        not null references calificaciones (id_calificacion),
    id_docente               bigint        not null references usuarios (id_usuario),
    id_periodo               bigint        not null references periodos_academicos (id_periodo),
    fuente                   varchar(20)   not null,
    nota_anterior            numeric(5, 3) not null,
    nota_nueva               numeric(5, 3) not null,
    estado_promedio_anterior varchar(20)   not null,
    promedio_anterior        numeric(3, 2),
    estado_promedio_nuevo    varchar(20)   not null,
    promedio_nuevo           numeric(3, 2),
    -- nulo solo para correcciones hechas por procesos del sistema, como el seed de desarrollo
    id_usuario               bigint references usuarios (id_usuario),
    motivo                   varchar(500),
    fecha_correccion         timestamp     not null default clock_timestamp(),
    constraint ck_correcciones_fuente check (fuente in ('ESTUDIANTES', 'COORDINACION', 'VICERRECTORIA')),
    constraint ck_correcciones_estados check (
        estado_promedio_anterior in ('COMPLETO', 'INCOMPLETO')
        and estado_promedio_nuevo in ('COMPLETO', 'INCOMPLETO')
    )
);

create index ix_correcciones_docente_periodo on correcciones_calificacion (id_docente, id_periodo);
