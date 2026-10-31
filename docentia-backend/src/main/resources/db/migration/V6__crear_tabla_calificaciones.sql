create table calificaciones (
    id_calificacion          bigint generated always as identity primary key,
    id_docente               bigint       not null references usuarios (id_usuario),
    id_periodo               bigint       not null references periodos_academicos (id_periodo),
    -- nulo en la fuente ESTUDIANTES, que consolida muchas respuestas y no tiene un evaluador único
    id_evaluador             bigint references usuarios (id_usuario),
    fuente                   varchar(20)  not null,
    dominio_disciplinar      numeric(3, 2) not null,
    planeacion_academica     numeric(3, 2) not null,
    metodologia_ensenanza    numeric(3, 2) not null,
    evaluacion_aprendizaje   numeric(3, 2) not null,
    compromiso_institucional numeric(3, 2) not null,
    observacion              varchar(1000),
    fecha_registro           timestamp    not null default now(),
    fecha_actualizacion      timestamp    not null default now(),
    constraint uq_calificaciones_docente_periodo_fuente unique (id_docente, id_periodo, fuente),
    constraint ck_calificaciones_fuente check (fuente in ('ESTUDIANTES', 'COORDINACION', 'VICERRECTORIA')),
    constraint ck_calificaciones_rango check (
        dominio_disciplinar between 1 and 5
        and planeacion_academica between 1 and 5
        and metodologia_ensenanza between 1 and 5
        and evaluacion_aprendizaje between 1 and 5
        and compromiso_institucional between 1 and 5
    )
);

create index ix_calificaciones_periodo on calificaciones (id_periodo);
