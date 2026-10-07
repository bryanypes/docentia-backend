create table facultades (
    id_facultad    bigint generated always as identity primary key,
    nombre         varchar(120) not null,
    es_activa      boolean      not null default true,
    fecha_creacion timestamp    not null default now()
);

create unique index ux_facultades_nombre on facultades (lower(nombre)) where es_activa;
