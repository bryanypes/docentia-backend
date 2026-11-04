-- Análisis de comentarios de estudiantes: fuente de la solicitud, resultado estructurado versionado
-- y la copia anonimizada de lo que se analizó. Se numera 34 porque V29 a V33 están reservadas por
-- otra rama en revisión.

alter table solicitudes_analisis
    add column fuente varchar(20) not null default 'CSV',
    -- detalle del estado INSUFICIENTE: POCAS_ENCUESTAS, SIN_COMENTARIOS o TODOS_EXCLUIDOS
    add column motivo varchar(30),
    add constraint ck_solicitudes_analisis_fuente check (fuente in ('CSV', 'FORMULARIO'));

-- Un resultado por solicitud completada. El vigente de un docente y periodo es el que no está
-- reemplazado; una corrida nueva marca la anterior como reemplazada en la misma transacción.
create table resultados_analisis_comentarios (
    id             bigint generated always as identity primary key,
    id_solicitud   bigint       not null unique references solicitudes_analisis (id_solicitud) on delete cascade,
    id_docente     bigint       not null references usuarios (id_usuario),
    id_periodo     bigint       not null references periodos_academicos (id_periodo),
    fuente         varchar(20)  not null,
    version        int          not null,
    reemplazado    boolean      not null default false,
    tipo_modelo    varchar(10)  not null,
    version_modelo varchar(80)  not null,
    version_lexico varchar(80)  not null,
    n_comentarios  int          not null,
    n_utiles       int          not null,
    resultado      text         not null,
    generado_en    timestamp    not null default now(),
    constraint uq_resultados_version unique (id_docente, id_periodo, version),
    constraint ck_resultados_tipo check (tipo_modelo in ('REGLAS', 'LLM'))
);

create unique index uq_resultados_vigente
    on resultados_analisis_comentarios (id_docente, id_periodo) where not reemplazado;
create index ix_resultados_periodo on resultados_analisis_comentarios (id_periodo) where not reemplazado;

-- Copia de lo analizado: solo texto anonimizado, su huella y lo que el modelo dijo de él. Sin fecha,
-- sin secuencia de llegada y sin el id de la respuesta; el orden es el de la huella.
create table comentarios_analizados (
    id_resultado bigint        not null references resultados_analisis_comentarios (id) on delete cascade,
    indice       int           not null,
    -- identificador con el que el modelo lo cita: el id del CSV, o la posición si viene del formulario
    id_cita      bigint        not null,
    hash         char(64)      not null,
    texto        varchar(1000) not null,
    sentimiento  varchar(10)   not null,
    etiquetas    text          not null,
    primary key (id_resultado, indice)
);

create index ix_comentarios_analizados_cita on comentarios_analizados (id_resultado, id_cita);
