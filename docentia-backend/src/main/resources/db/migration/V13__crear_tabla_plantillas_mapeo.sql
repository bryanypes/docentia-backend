-- Mapeo de columnas del CSV guardado por un coordinador para reutilizarlo en el siguiente periodo.
-- columnas y mapeo son JSON: la lista de encabezados con que se creó y el objeto columna -> campo.
create table plantillas_mapeo (
    id_plantilla        bigint generated always as identity primary key,
    id_usuario          bigint       not null references usuarios (id_usuario),
    nombre              varchar(100) not null,
    columnas            text         not null,
    mapeo               text         not null,
    fecha_creacion      timestamp    not null default clock_timestamp(),
    fecha_actualizacion timestamp    not null default clock_timestamp(),
    constraint uq_plantillas_mapeo_usuario_nombre unique (id_usuario, nombre)
);
