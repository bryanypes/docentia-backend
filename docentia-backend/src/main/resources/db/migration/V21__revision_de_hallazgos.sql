-- E4: la coordinación incluye, descarta o edita cada hallazgo. Cada cambio deja una versión con el
-- modelo y la instrucción que generaron el borrador.
alter table hallazgos_analisis drop constraint ck_hallazgos_estado;
alter table hallazgos_analisis add constraint ck_hallazgos_estado
    check (estado in ('PENDIENTE_VALIDACION', 'SIN_CITA_VERIFICADA', 'VALIDADO', 'DESCARTADO'));

create table versiones_hallazgo (
    id_version          bigint generated always as identity primary key,
    id_hallazgo         bigint        not null references hallazgos_analisis (id_hallazgo),
    version             integer       not null,
    accion              varchar(20)   not null,
    texto               varchar(1000) not null,
    estado              varchar(30)   not null,
    modelo              varchar(100),
    version_instruccion varchar(40),
    -- nulo en la versión que generó el análisis
    id_usuario          bigint references usuarios (id_usuario),
    motivo              varchar(500),
    fecha               timestamp     not null default clock_timestamp(),
    constraint uq_versiones_hallazgo unique (id_hallazgo, version),
    constraint ck_versiones_accion check (accion in ('GENERADO', 'INCLUIDO', 'DESCARTADO', 'EDITADO'))
);
