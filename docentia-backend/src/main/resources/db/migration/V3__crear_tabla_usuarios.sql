create table usuarios (
    id_usuario      bigint generated always as identity primary key,
    nombre_completo varchar(150) not null,
    correo          varchar(150) not null,
    contrasena_hash varchar(100) not null,
    rol             varchar(20)  not null,
    id_facultad     bigint references facultades (id_facultad),
    id_programa     bigint references programas (id_programa),
    es_activo       boolean      not null default true,
    fecha_creacion  timestamp    not null default now(),
    constraint ck_usuarios_rol check (rol in ('COORDINADOR', 'VICERRECTOR', 'DOCENTE')),
    -- el coordinador responde por una facultad, el docente pertenece a un programa
    -- y el vicerrector no tiene restricción de alcance
    constraint ck_usuarios_alcance check (
        (rol = 'COORDINADOR' and id_facultad is not null and id_programa is null)
        or (rol = 'DOCENTE' and id_programa is not null and id_facultad is null)
        or (rol = 'VICERRECTOR' and id_facultad is null and id_programa is null)
    )
);

create unique index ux_usuarios_correo on usuarios (lower(correo));
create index ix_usuarios_programa on usuarios (id_programa);
