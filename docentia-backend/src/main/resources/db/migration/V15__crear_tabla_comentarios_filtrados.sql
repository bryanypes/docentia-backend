-- Comentarios que el filtro excluyó de una solicitud de análisis antes de armar la petición al
-- modelo: ofensivos o sobre rasgos personales del docente. No se envían, no se citan y quedan aquí
-- con la regla que los excluyó.
create table comentarios_filtrados (
    id_filtrado   bigint generated always as identity primary key,
    id_solicitud  bigint       not null references solicitudes_analisis (id_solicitud) on delete cascade,
    id_comentario bigint       not null references comentarios_estudiantes (id_comentario),
    categoria     varchar(40)  not null,
    regla         varchar(300) not null,
    fecha_filtro  timestamp    not null default clock_timestamp(),
    constraint uq_comentarios_filtrados unique (id_solicitud, id_comentario)
);

alter table solicitudes_analisis add column comentarios_filtrados integer;
