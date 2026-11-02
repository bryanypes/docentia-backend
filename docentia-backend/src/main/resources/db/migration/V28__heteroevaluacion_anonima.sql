-- Heteroevaluación anónima. La participación (quién ya respondió) y la respuesta (qué respondió)
-- viven en tablas sin ninguna columna que las una: la respuesta no tiene estudiante, fecha ni
-- secuencia, y su id es un UUID aleatorio.

-- La asignación (clase + docente) necesita un id propio para la API.
alter table clases_docente add column id bigint generated always as identity;
alter table clases_docente add constraint uq_clases_docente_id unique (id);
create index ix_clases_docente_docente on clases_docente (docente_id);

create table participaciones_heteroevaluacion (
    asignacion_id bigint    not null references clases_docente (id),
    estudiante_id bigint    not null references usuarios (id_usuario),
    enviada_en    timestamp not null,
    primary key (asignacion_id, estudiante_id)
);

create index ix_participaciones_estudiante on participaciones_heteroevaluacion (estudiante_id);

create table respuestas_heteroevaluacion (
    id                  uuid          primary key default gen_random_uuid(),
    asignacion_id       bigint        not null references clases_docente (id),
    docente_id          bigint        not null references usuarios (id_usuario),
    periodo_id          bigint        not null references periodos_academicos (id_periodo),
    instrumento_version varchar(40)   not null,
    comentario          varchar(1000)
);

create index ix_respuestas_hetero_docente on respuestas_heteroevaluacion (periodo_id, docente_id);
create index ix_respuestas_hetero_asignacion on respuestas_heteroevaluacion (asignacion_id);

create table respuestas_descriptor_hetero (
    respuesta_id  uuid     not null references respuestas_heteroevaluacion (id) on delete cascade,
    descriptor_id bigint   not null references descriptores_evaluacion (id),
    valor         smallint not null,
    primary key (respuesta_id, descriptor_id),
    constraint ck_respuestas_hetero_valor check (valor between 1 and 5)
);

-- El esquema anterior guardaba al estudiante junto a sus puntajes y publicaba sus comentarios al
-- enviar. Esos datos solo existían en desarrollo y no se migran: no son anónimos.
delete from comentarios_estudiantes where id_importacion is null;
delete from evaluaciones_formulario where tipo = 'HETERO';
