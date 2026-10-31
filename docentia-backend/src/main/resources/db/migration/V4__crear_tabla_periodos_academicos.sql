create table periodos_academicos (
    id_periodo     bigint generated always as identity primary key,
    nombre         varchar(20) not null,
    fecha_inicio   date        not null,
    fecha_fin      date        not null,
    es_activo      boolean     not null default false,
    fecha_creacion timestamp   not null default now(),
    constraint uq_periodos_academicos_nombre unique (nombre),
    constraint ck_periodos_academicos_fechas check (fecha_inicio <= fecha_fin)
);

-- el índice parcial solo admite una fila con es_activo = true, así dos activaciones
-- simultáneas no pueden dejar dos periodos activos
create unique index ux_periodos_academicos_activo on periodos_academicos (es_activo) where es_activo;
