-- Corregir una autoevaluación o coevaluación ya enviada: quien la envió pide, su superior verifica
-- y la administración reabre el formulario.
alter table evaluaciones_formulario add column reabierto boolean not null default false;

create table solicitudes_reapertura (
    id                      bigint generated always as identity primary key,
    id_formulario           bigint       not null references evaluaciones_formulario (id),
    id_solicitante          bigint       not null references usuarios (id_usuario),
    motivo                  varchar(500) not null,
    estado                  varchar(25)  not null,
    rol_aprobador           varchar(20),
    id_aprobador            bigint references usuarios (id_usuario),
    respuesta_aprobador     varchar(500),
    fecha_aprobacion        timestamp,
    id_administrador        bigint references usuarios (id_usuario),
    respuesta_administrador varchar(500),
    fecha_atencion          timestamp,
    fecha_solicitud         timestamp    not null,
    version                 bigint       not null default 0,
    constraint ck_reapertura_estado
        check (estado in ('PENDIENTE_APROBACION', 'APROBADA', 'RECHAZADA', 'ATENDIDA')),
    constraint ck_reapertura_rol check (rol_aprobador in ('COORDINADOR', 'VICERRECTOR'))
);

-- Una sola solicitud abierta por formulario.
create unique index uq_reapertura_abierta on solicitudes_reapertura (id_formulario)
    where estado in ('PENDIENTE_APROBACION', 'APROBADA');
create index ix_reapertura_solicitante on solicitudes_reapertura (id_solicitante);
