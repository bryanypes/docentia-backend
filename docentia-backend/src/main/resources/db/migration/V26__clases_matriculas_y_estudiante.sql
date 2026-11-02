-- HU-01 etapa 1: códigos Campus, rol ESTUDIANTE y grafo clase–matrícula.
-- El formulario HETERO y el corte del CSV no entran en esta migración.

alter table facultades add column codigo varchar(40);
create unique index ux_facultades_codigo on facultades (lower(codigo))
    where es_activa and codigo is not null;

alter table programas add column codigo varchar(40);
create unique index ux_programas_codigo on programas (lower(codigo))
    where es_activo and codigo is not null;

alter table usuarios add column codigo varchar(40);

alter table usuarios drop constraint ck_usuarios_rol;
alter table usuarios add constraint ck_usuarios_rol
    check (rol in ('COORDINADOR', 'VICERRECTOR', 'DOCENTE', 'ESTUDIANTE'));

alter table usuarios drop constraint ck_usuarios_alcance;
alter table usuarios add constraint ck_usuarios_alcance check (
    (rol = 'COORDINADOR' and id_facultad is not null and id_programa is null)
    or (rol = 'DOCENTE' and id_programa is not null and id_facultad is null)
    or (rol = 'ESTUDIANTE' and id_programa is not null and id_facultad is null)
    or (rol = 'VICERRECTOR' and id_facultad is null and id_programa is null)
);

create table cursos (
    id           bigint generated always as identity primary key,
    codigo       varchar(40)  not null,
    nombre       varchar(200) not null,
    id_programa  bigint       not null references programas (id_programa),
    es_activa    boolean      not null default true,
    constraint uq_cursos_codigo unique (codigo)
);

create index ix_cursos_programa on cursos (id_programa);

create table clases (
    id                    bigint generated always as identity primary key,
    id_periodo            bigint      not null references periodos_academicos (id_periodo),
    id_curso              bigint      not null references cursos (id),
    grupo                 varchar(10) not null,
    jornada               varchar(20) not null,
    tipo_curso            varchar(20) not null,
    inscritos_esperados   integer,
    constraint uq_clases_oferta unique (id_periodo, id_curso, grupo, jornada)
);

create index ix_clases_periodo on clases (id_periodo);
create index ix_clases_curso on clases (id_curso);

create table clases_docente (
    clase_id   bigint not null references clases (id),
    docente_id bigint not null references usuarios (id_usuario),
    primary key (clase_id, docente_id)
);

create table matriculas (
    id            bigint generated always as identity primary key,
    clase_id      bigint  not null references clases (id),
    estudiante_id bigint  not null references usuarios (id_usuario),
    es_activa     boolean not null default true
);

create unique index ux_matriculas_activa on matriculas (clase_id, estudiante_id) where es_activa;
create index ix_matriculas_estudiante on matriculas (estudiante_id);
