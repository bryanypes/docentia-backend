-- Instrumento del Anexo 1 de la Resolución 074 de 2026 y respuestas de sus formularios.
create table criterios_evaluacion (
    id                  bigint generated always as identity primary key,
    codigo              varchar(5)   not null,
    orden               integer      not null,
    nombre              varchar(150) not null,
    instrumento_version varchar(40)  not null default 'RES074-2026-ANEXO1',
    constraint uq_criterios_codigo unique (instrumento_version, codigo)
);

create table descriptores_evaluacion (
    id                  bigint generated always as identity primary key,
    codigo              varchar(5)   not null,
    orden               integer      not null,
    texto               varchar(400) not null,
    criterio_id         bigint       not null references criterios_evaluacion (id),
    instrumento_version varchar(40)  not null default 'RES074-2026-ANEXO1',
    constraint uq_descriptores_codigo unique (instrumento_version, codigo)
);

create index ix_descriptores_criterio on descriptores_evaluacion (criterio_id);

create table descriptor_actor (
    descriptor_id bigint      not null references descriptores_evaluacion (id),
    tipo          varchar(10) not null,
    primary key (descriptor_id, tipo),
    constraint ck_descriptor_actor check (tipo in ('AUTO', 'COEVAL', 'HETERO'))
);

create table evaluaciones_formulario (
    id                   bigint generated always as identity primary key,
    periodo_id           bigint       not null references periodos_academicos (id_periodo),
    docente_id           bigint       not null references usuarios (id_usuario),
    evaluador_usuario_id bigint       not null references usuarios (id_usuario),
    tipo                 varchar(10)  not null,
    estado               varchar(10)  not null default 'BORRADOR',
    instrumento_version  varchar(40)  not null,
    valor                numeric(6, 4),
    enviada_en           timestamp,
    version              bigint       not null default 0,
    constraint uq_evaluaciones_formulario unique (periodo_id, docente_id, tipo),
    constraint ck_evaluaciones_tipo check (tipo in ('AUTO', 'COEVAL')),
    constraint ck_evaluaciones_estado check (estado in ('BORRADOR', 'ENVIADA')),
    constraint ck_evaluaciones_envio check (
        (estado = 'BORRADOR' and enviada_en is null and valor is null)
        or (estado = 'ENVIADA' and enviada_en is not null and valor is not null)
    )
);

create index ix_evaluaciones_docente on evaluaciones_formulario (docente_id);
create index ix_evaluaciones_evaluador on evaluaciones_formulario (evaluador_usuario_id);

create table respuestas_descriptor (
    evaluacion_id bigint   not null references evaluaciones_formulario (id) on delete cascade,
    descriptor_id bigint   not null references descriptores_evaluacion (id),
    valor         smallint not null,
    constraint uq_respuestas_descriptor unique (evaluacion_id, descriptor_id),
    constraint ck_respuestas_valor check (valor between 1 and 5)
);

-- Solo acompañan el formulario: no van al análisis ni a los avisos.
create table comentarios_criterio (
    evaluacion_id bigint        not null references evaluaciones_formulario (id) on delete cascade,
    criterio_id   bigint        not null references criterios_evaluacion (id),
    texto         varchar(1000) not null,
    constraint uq_comentarios_criterio unique (evaluacion_id, criterio_id)
);

-- De dónde salió cada fuente. La nota exacta solo existe si vino de un formulario.
alter table calificaciones add column origen varchar(20) not null default 'MANUAL';
alter table calificaciones add column nota_fuente numeric(6, 4);
alter table calificaciones add constraint ck_calificaciones_origen
    check (origen in ('MANUAL', 'CSV', 'FORMULARIO'));
update calificaciones set origen = 'CSV' where id_importacion is not null;

insert into criterios_evaluacion (codigo, orden, nombre) values ('C1', 1, 'Capacidad para planear procesos de docencia');
insert into criterios_evaluacion (codigo, orden, nombre) values ('C2', 2, 'Capacidad para ejecutar procesos de docencia');
insert into criterios_evaluacion (codigo, orden, nombre) values ('C3', 3, 'Capacidad para evaluar procesos de docencia');
insert into criterios_evaluacion (codigo, orden, nombre) values ('C4', 4, 'Sensibilidad social');
insert into criterios_evaluacion (codigo, orden, nombre) values ('C5', 5, 'Creatividad, dinamismo e innovación');
insert into criterios_evaluacion (codigo, orden, nombre) values ('C6', 6, 'Capacidad para aprender, desaprender y reaprender');

insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D01', 1, 'Prepara el desarrollo del curso evidenciando los resultados de aprendizaje, las estrategias formativas, los recursos y materiales, y los mecanismos de seguimiento y de evaluación', id from criterios_evaluacion where codigo = 'C1';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D01';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'COEVAL' from descriptores_evaluacion where codigo = 'D01';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D02', 2, 'Socializa entre los estudiantes los resultados de aprendizaje, las estrategias formativas y los mecanismos de seguimiento y de evaluación', id from criterios_evaluacion where codigo = 'C2';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D02';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'HETERO' from descriptores_evaluacion where codigo = 'D02';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D03', 3, 'Promueve, a través de las actividades formativas, el desarrollo de actitudes emprendedoras, de responsabilidad social y ciudadana o de respeto por el medio ambiente', id from criterios_evaluacion where codigo = 'C2';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D03';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'HETERO' from descriptores_evaluacion where codigo = 'D03';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D04', 4, 'Promueve, a través de las actividades formativas, el desarrollo de capacidades para la investigación, la creación y el pensamiento crítico', id from criterios_evaluacion where codigo = 'C2';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D04';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'HETERO' from descriptores_evaluacion where codigo = 'D04';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D05', 5, 'Expresa con claridad las ideas, conceptos e instrucciones en el desarrollo de las actividades formativas', id from criterios_evaluacion where codigo = 'C2';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D05';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'HETERO' from descriptores_evaluacion where codigo = 'D05';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D06', 6, 'Desarrolla procesos evaluativos alineados con los resultados de aprendizaje', id from criterios_evaluacion where codigo = 'C3';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D06';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'HETERO' from descriptores_evaluacion where codigo = 'D06';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'COEVAL' from descriptores_evaluacion where codigo = 'D06';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D07', 7, 'Proporciona retroalimentación a los estudiantes sobre los resultados obtenidos en las actividades evaluativas', id from criterios_evaluacion where codigo = 'C3';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D07';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'HETERO' from descriptores_evaluacion where codigo = 'D07';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D08', 8, 'Realiza actividades de acompañamiento y seguimiento para reforzar el aprendizaje de los estudiantes', id from criterios_evaluacion where codigo = 'C2';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D08';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'HETERO' from descriptores_evaluacion where codigo = 'D08';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D09', 9, 'Establece y promueve relaciones respetuosas y cordiales con y entre los estudiantes en el desarrollo del proceso formativo', id from criterios_evaluacion where codigo = 'C4';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D09';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'HETERO' from descriptores_evaluacion where codigo = 'D09';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D10', 10, 'Desarrolla las actividades formativas teniendo en cuenta las necesidades, dificultades y fortalezas de los estudiantes', id from criterios_evaluacion where codigo = 'C4';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D10';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'HETERO' from descriptores_evaluacion where codigo = 'D10';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D11', 11, 'Genera y desarrolla actividades creativas e innovadoras que dinamizan el aprendizaje de los estudiantes', id from criterios_evaluacion where codigo = 'C5';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D11';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'HETERO' from descriptores_evaluacion where codigo = 'D11';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'COEVAL' from descriptores_evaluacion where codigo = 'D11';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D12', 12, 'Incorpora el uso de metodologías, herramientas y tecnologías en el desarrollo de las actividades formativas', id from criterios_evaluacion where codigo = 'C5';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D12';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'HETERO' from descriptores_evaluacion where codigo = 'D12';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'COEVAL' from descriptores_evaluacion where codigo = 'D12';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D13', 13, 'Genera informes que analizan las fortalezas y oportunidades de mejoramiento en relación con la planeación y desarrollo de los cursos, las metodologías y el logro de los resultados de aprendizaje', id from criterios_evaluacion where codigo = 'C3';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D13';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'COEVAL' from descriptores_evaluacion where codigo = 'D13';
insert into descriptores_evaluacion (codigo, orden, texto, criterio_id)
    select 'D14', 14, 'Implementa en su práctica docente las acciones de los planes de mejoramiento, las capacidades generadas a través de los procesos de formación docente y los aprendizajes derivados de las dinámicas de la docencia', id from criterios_evaluacion where codigo = 'C6';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'AUTO' from descriptores_evaluacion where codigo = 'D14';
insert into descriptor_actor (descriptor_id, tipo)
    select id, 'COEVAL' from descriptores_evaluacion where codigo = 'D14';
