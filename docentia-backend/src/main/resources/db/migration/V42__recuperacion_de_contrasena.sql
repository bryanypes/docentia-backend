-- Recuperación de contraseña: se guarda solo el hash SHA-256 del token, que vence y sirve una vez.
-- Si el PR de roles (#12) llega con otra V42, se renumera allá.
create table recuperaciones_contrasena (
    id          bigint generated always as identity primary key,
    id_usuario  bigint      not null references usuarios (id_usuario) on delete cascade,
    hash_token  varchar(64) not null,
    vence_en    timestamp   not null,
    usado_en    timestamp,
    creado_en   timestamp   not null default clock_timestamp(),
    constraint uq_recuperaciones_hash unique (hash_token)
);

create index ix_recuperaciones_usuario on recuperaciones_contrasena (id_usuario);
