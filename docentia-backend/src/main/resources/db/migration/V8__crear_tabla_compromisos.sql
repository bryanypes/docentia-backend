create table compromisos (
    id_compromiso  bigint generated always as identity primary key,
    id_docente     bigint       not null references usuarios (id_usuario),
    id_periodo     bigint       not null references periodos_academicos (id_periodo),
    descripcion    varchar(500) not null,
    fecha_limite   date         not null,
    estado         varchar(20)  not null default 'VIGENTE',
    fecha_creacion timestamp    not null default now(),
    constraint ck_compromisos_estado check (estado in ('VIGENTE', 'CUMPLIDO'))
);

create index ix_compromisos_docente_estado on compromisos (id_docente, estado);
