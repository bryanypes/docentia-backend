create table etapas_periodo (
    id_etapa_periodo bigint generated always as identity primary key,
    id_periodo       bigint      not null references periodos_academicos (id_periodo) on delete cascade,
    tipo_etapa       varchar(40) not null,
    fecha_apertura   date        not null,
    fecha_cierre     date        not null,
    constraint uq_etapas_periodo_tipo unique (id_periodo, tipo_etapa),
    constraint ck_etapas_periodo_tipo check (tipo_etapa in (
        'EVALUACION_ESTUDIANTES',
        'CALIFICACION_COORDINACION',
        'CALIFICACION_VICERRECTORIA',
        'RETROALIMENTACION'
    )),
    constraint ck_etapas_periodo_fechas check (fecha_apertura <= fecha_cierre)
);
