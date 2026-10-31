create table programas (
    id_programa    bigint generated always as identity primary key,
    id_facultad    bigint       not null references facultades (id_facultad),
    nombre         varchar(120) not null,
    es_activo      boolean      not null default true,
    fecha_creacion timestamp    not null default now()
);

create unique index ux_programas_facultad_nombre on programas (id_facultad, lower(nombre)) where es_activo;
create index ix_programas_facultad on programas (id_facultad);
