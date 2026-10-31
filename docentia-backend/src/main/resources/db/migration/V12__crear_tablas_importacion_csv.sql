-- Carga manual del CSV de evaluación de estudiantes exportado de Power BI.

-- Cada intento de importar queda registrado, salga bien o no, para auditar de dónde salió cada nota.
create table importaciones (
    id_importacion        bigint generated always as identity primary key,
    nombre_archivo        varchar(255) not null,
    hash_sha256           varchar(64)  not null,
    id_periodo            bigint       not null references periodos_academicos (id_periodo),
    id_usuario            bigint       not null references usuarios (id_usuario),
    fecha_importacion     timestamp    not null default clock_timestamp(),
    resultado             varchar(20)  not null,
    codificacion          varchar(20),
    delimitador           varchar(1),
    filas_procesadas      integer      not null,
    filas_con_error       integer      not null,
    docentes_afectados    integer      not null default 0,
    notas_nuevas          integer      not null default 0,
    notas_corregidas      integer      not null default 0,
    notas_sin_cambios     integer      not null default 0,
    comentarios_nuevos    integer      not null default 0,
    comentarios_repetidos integer      not null default 0,
    constraint ck_importaciones_resultado check (resultado in ('EXITOSA', 'RECHAZADA'))
);

create index ix_importaciones_periodo on importaciones (id_periodo);
create index ix_importaciones_hash on importaciones (hash_sha256);

-- La nota de estudiantes recuerda la carga que fijó su valor actual.
alter table calificaciones add column id_importacion bigint references importaciones (id_importacion);

-- Power BI entrega la escala de 0 a 5; la calificación de coordinación y vicerrectoría sigue
-- validándose de 1 a 5 en la API.
alter table calificaciones drop constraint ck_calificaciones_rango;
alter table calificaciones add constraint ck_calificaciones_rango check (
    dominio_disciplinar between 0 and 5
    and planeacion_academica between 0 and 5
    and metodologia_ensenanza between 0 and 5
    and evaluacion_aprendizaje between 0 and 5
    and compromiso_institucional between 0 and 5
);

-- Comentarios abiertos de los estudiantes. La clave de negocio es docente + periodo + hash del
-- texto normalizado y de su número de aparición en el archivo, así recargar el mismo archivo no
-- duplica nada y dos estudiantes que escribieron lo mismo no se funden en uno.
create table comentarios_estudiantes (
    id_comentario   bigint generated always as identity primary key,
    id_docente      bigint        not null references usuarios (id_usuario),
    id_periodo      bigint        not null references periodos_academicos (id_periodo),
    texto           varchar(2000) not null,
    hash_texto      varchar(64)   not null,
    id_importacion  bigint        not null references importaciones (id_importacion),
    fecha_registro  timestamp     not null default clock_timestamp(),
    constraint uq_comentarios_docente_periodo_hash unique (id_docente, id_periodo, hash_texto)
);
