-- Solicitudes de análisis de los comentarios de un docente. Viven en la base para que un reinicio no
-- las pierda: el procesador en segundo plano toma las que están EN_COLA y retoma las EN_PROCESO.
create table solicitudes_analisis (
    id_solicitud        bigint generated always as identity primary key,
    id_docente          bigint        not null references usuarios (id_usuario),
    id_periodo          bigint        not null references periodos_academicos (id_periodo),
    id_solicitante      bigint        not null references usuarios (id_usuario),
    estado              varchar(20)   not null default 'EN_COLA',
    -- texto para el usuario cuando el estado es ERROR o INSUFICIENTE
    mensaje             varchar(500),
    encuestas_cerradas  integer,
    comentarios_enviados integer,
    intentos            integer       not null default 0,
    -- con qué se generó el borrador
    modelo              varchar(100),
    version_instruccion varchar(40),
    fecha_borrador      timestamp,
    -- PENDIENTE_VALIDACION mientras el coordinador no valide (E4); el docente no lo ve antes
    estado_borrador     varchar(30),
    fecha_solicitud     timestamp     not null default clock_timestamp(),
    fecha_inicio        timestamp,
    fecha_fin           timestamp,
    constraint ck_solicitudes_analisis_estado
        check (estado in ('EN_COLA', 'EN_PROCESO', 'COMPLETADO', 'ERROR', 'INSUFICIENTE')),
    constraint ck_solicitudes_analisis_borrador
        check (estado_borrador is null or estado_borrador in ('PENDIENTE_VALIDACION', 'VALIDADO'))
);

-- Una sola solicitud en curso por docente y periodo, aunque lleguen dos peticiones al mismo tiempo.
create unique index ux_solicitudes_analisis_en_curso
    on solicitudes_analisis (id_docente, id_periodo)
    where estado in ('EN_COLA', 'EN_PROCESO');

create index ix_solicitudes_analisis_pendientes on solicitudes_analisis (estado, id_solicitud);

create table hallazgos_analisis (
    id_hallazgo  bigint generated always as identity primary key,
    id_solicitud bigint        not null references solicitudes_analisis (id_solicitud) on delete cascade,
    orden        integer       not null,
    tipo         varchar(30)   not null,
    texto        varchar(1000) not null,
    estado       varchar(30)   not null default 'PENDIENTE_VALIDACION',
    constraint ck_hallazgos_tipo check (tipo in ('FORTALEZA', 'OPORTUNIDAD_DE_MEJORA')),
    constraint ck_hallazgos_estado check (estado in ('PENDIENTE_VALIDACION'))
);

create index ix_hallazgos_solicitud on hallazgos_analisis (id_solicitud);

-- Cita textual que respalda un hallazgo. El identificador del comentario es el que devolvió el
-- modelo y puede no existir: por eso no lleva llave foránea.
create table citas_hallazgo (
    id_cita              bigint generated always as identity primary key,
    id_hallazgo          bigint        not null references hallazgos_analisis (id_hallazgo) on delete cascade,
    id_comentario_citado bigint,
    fragmento            varchar(1000) not null
);

create index ix_citas_hallazgo on citas_hallazgo (id_hallazgo);
