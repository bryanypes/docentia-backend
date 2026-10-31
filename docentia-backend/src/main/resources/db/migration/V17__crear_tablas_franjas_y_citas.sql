-- Franjas de 30 minutos que el coordinador abre para atender citas de retroalimentación, y las citas
-- que los docentes reservan en ellas. La tabla de citas se crea aquí porque las franjas necesitan
-- saber si están reservadas para no cerrarse ni borrarse con una cita encima.

-- btree_gist permite mezclar igualdad (mismo coordinador) y solape de rangos en una restricción.
create extension if not exists btree_gist;

create table franjas_horarias (
    id_franja      bigint generated always as identity primary key,
    id_coordinador bigint      not null references usuarios (id_usuario),
    id_periodo     bigint      not null references periodos_academicos (id_periodo),
    fecha          date        not null,
    hora_inicio    time        not null,
    hora_fin       time        not null,
    estado         varchar(20) not null default 'ABIERTA',
    fecha_creacion timestamp   not null default clock_timestamp(),
    constraint ck_franjas_estado check (estado in ('ABIERTA', 'CERRADA')),
    -- duración fija de 30 minutos, sin pasar de medianoche
    constraint ck_franjas_duracion check (
        hora_inicio <= time '23:30' and hora_fin = hora_inicio + interval '30 minutes'
    ),
    -- dos franjas abiertas del mismo coordinador no pueden solaparse, aunque lleguen al mismo tiempo
    constraint ex_franjas_sin_solape exclude using gist (
        id_coordinador with =,
        tsrange(fecha + hora_inicio, fecha + hora_fin) with &&
    ) where (estado = 'ABIERTA')
);

create index ix_franjas_coordinador_fecha on franjas_horarias (id_coordinador, fecha);

create table citas (
    id_cita           bigint generated always as identity primary key,
    id_franja         bigint      not null references franjas_horarias (id_franja),
    id_docente        bigint      not null references usuarios (id_usuario),
    id_periodo        bigint      not null references periodos_academicos (id_periodo),
    estado            varchar(20) not null default 'ACTIVA',
    -- por qué dejó de estar activa: REEMPLAZADA (el docente reservó otra) o LIBERADA_POR_DOCENTE (F3)
    motivo_liberacion varchar(30),
    fecha_reserva     timestamp   not null default clock_timestamp(),
    fecha_liberacion  timestamp,
    constraint ck_citas_estado check (estado in ('ACTIVA', 'LIBERADA')),
    constraint ck_citas_motivo check (
        motivo_liberacion is null or motivo_liberacion in ('REEMPLAZADA', 'LIBERADA_POR_DOCENTE')
    )
);

-- Un docente tiene a lo sumo una cita activa por periodo.
create unique index ux_citas_docente_periodo_activa on citas (id_docente, id_periodo) where estado = 'ACTIVA';
-- Una franja tiene a lo sumo una cita activa.
create unique index ux_citas_franja_activa on citas (id_franja) where estado = 'ACTIVA';
