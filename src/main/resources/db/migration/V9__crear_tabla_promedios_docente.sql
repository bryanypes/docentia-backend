create table promedios_docente (
    id_promedio        bigint generated always as identity primary key,
    id_docente         bigint        not null references usuarios (id_usuario),
    id_periodo         bigint        not null references periodos_academicos (id_periodo),
    -- nulo mientras falte alguna fuente: un promedio parcial se confundiría con el definitivo
    valor              numeric(3, 2),
    estado             varchar(20)   not null,
    -- fuentes separadas por coma, en el orden de FuenteEvaluacion; nulo cuando está completo
    fuentes_pendientes varchar(100),
    fecha_calculo      timestamp     not null,
    constraint uq_promedios_docente_periodo unique (id_docente, id_periodo),
    constraint ck_promedios_estado check (estado in ('COMPLETO', 'INCOMPLETO')),
    constraint ck_promedios_coherencia check (
        (estado = 'COMPLETO' and valor between 1 and 5 and fuentes_pendientes is null)
        or (estado = 'INCOMPLETO' and valor is null and fuentes_pendientes is not null)
    )
);

create index ix_promedios_docente_periodo on promedios_docente (id_periodo);
