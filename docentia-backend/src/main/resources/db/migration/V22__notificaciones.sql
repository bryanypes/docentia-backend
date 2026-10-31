-- Avisos dentro de la app y outbox de correo. Los textos son genéricos: nunca llevan notas,
-- categorías, comentarios ni hallazgos.
create table notificaciones (
    id           bigint generated always as identity primary key,
    usuario_id   bigint        not null references usuarios (id_usuario) on delete cascade,
    tipo         varchar(40)   not null,
    titulo       varchar(150)  not null,
    mensaje      varchar(500)  not null,
    enlace       varchar(200),
    entidad_tipo varchar(40),
    entidad_id   bigint,
    clave_dedupe varchar(150)  not null,
    leida        boolean       not null default false,
    leida_en     timestamp,
    creada_en    timestamp     not null default clock_timestamp(),
    constraint uq_notificaciones_dedupe unique (usuario_id, tipo, clave_dedupe)
);

create index ix_notificaciones_bandeja on notificaciones (usuario_id, leida, creada_en desc);

create table cola_correo (
    id               bigint generated always as identity primary key,
    notificacion_id  bigint references notificaciones (id) on delete set null,
    destinatario     varchar(150)  not null,
    asunto           varchar(200)  not null,
    cuerpo           varchar(2000) not null,
    estado           varchar(20)   not null default 'PENDIENTE',
    intentos         integer       not null default 0,
    proximo_intento  timestamp     not null default clock_timestamp(),
    ultimo_error     varchar(200),
    creada_en        timestamp     not null default clock_timestamp(),
    enviada_en       timestamp,
    constraint ck_cola_correo_estado check (estado in ('PENDIENTE', 'ENVIADO', 'ERROR'))
);

create index ix_cola_correo_pendientes on cola_correo (proximo_intento) where estado = 'PENDIENTE';
create index ix_cola_correo_notificacion on cola_correo (notificacion_id);

create table preferencias_notificacion (
    usuario_id        bigint primary key references usuarios (id_usuario) on delete cascade,
    correo_habilitado boolean not null default true
);
