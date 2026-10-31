-- Actas de retroalimentación (G1, G3, G4). El acta copia el promedio y las notas al firmarse, así
-- las correcciones posteriores no la tocan. Corregir un acta firmada crea una versión nueva.
create table actas (
    id_acta              bigint generated always as identity primary key,
    id_docente           bigint        not null references usuarios (id_usuario),
    id_periodo           bigint        not null references periodos_academicos (id_periodo),
    version              integer       not null default 1,
    estado               varchar(20)   not null default 'BORRADOR',
    promedio             numeric(3, 2),
    estado_promedio      varchar(20)   not null,
    categoria            varchar(20),
    nota_estudiantes     numeric(5, 3),
    nota_coordinacion    numeric(5, 3),
    nota_autoevaluacion  numeric(5, 3),
    umbral_compromisos   numeric(3, 2) not null,
    -- última corrección de notas del docente en el periodo cuando se congeló el acta
    id_ultima_correccion bigint references correcciones_calificacion (id_correccion),
    id_acta_anterior     bigint references actas (id_acta),
    motivo_correccion    varchar(500),
    id_usuario_corrige   bigint references usuarios (id_usuario),
    codigo_verificacion  varchar(20),
    hash_contenido       varchar(64),
    hash_pdf             varchar(64),
    pdf                  bytea,
    fecha_firma          timestamp,
    fecha_creacion       timestamp     not null default now(),
    constraint uq_actas_version unique (id_docente, id_periodo, version),
    constraint uq_actas_codigo unique (codigo_verificacion),
    constraint ck_actas_estado check (estado in ('BORRADOR', 'FIRMADA', 'ANULADA')),
    constraint ck_actas_firmada check (
        estado <> 'FIRMADA' or (pdf is not null and hash_pdf is not null and fecha_firma is not null)
    )
);

create unique index ux_actas_docente_periodo_vigente
    on actas (id_docente, id_periodo) where estado <> 'ANULADA';

create table firmas_acta (
    id_firma   bigint generated always as identity primary key,
    id_acta    bigint       not null references actas (id_acta),
    id_usuario bigint       not null references usuarios (id_usuario),
    rol        varchar(20)  not null,
    nombre     varchar(150) not null,
    correo     varchar(150) not null,
    ip         varchar(45)  not null,
    fecha      timestamp    not null,
    constraint uq_firmas_acta_rol unique (id_acta, rol),
    constraint ck_firmas_rol check (rol in ('COORDINADOR', 'DOCENTE'))
);

-- La inmutabilidad también vive en la base: un acta firmada solo puede pasar a ANULADA.
create function proteger_acta_firmada() returns trigger as $$
begin
    if tg_op = 'DELETE' then
        if old.estado <> 'BORRADOR' then
            raise exception 'Un acta firmada no se puede eliminar';
        end if;
        return old;
    end if;
    if old.estado = 'ANULADA' then
        raise exception 'Un acta anulada no se puede modificar';
    end if;
    if old.estado = 'FIRMADA' and (
        new.estado <> 'ANULADA'
        or new.pdf is distinct from old.pdf
        or new.hash_pdf is distinct from old.hash_pdf
        or new.hash_contenido is distinct from old.hash_contenido
        or new.codigo_verificacion is distinct from old.codigo_verificacion
        or new.promedio is distinct from old.promedio
        or new.categoria is distinct from old.categoria
        or new.fecha_firma is distinct from old.fecha_firma
    ) then
        raise exception 'Un acta firmada es inmutable';
    end if;
    return new;
end;
$$ language plpgsql;

create trigger tr_actas_inmutables before update or delete on actas
    for each row execute function proteger_acta_firmada();

alter table compromisos add column id_acta bigint references actas (id_acta);
alter table compromisos add column observacion varchar(500);
create index ix_compromisos_acta on compromisos (id_acta);

-- Cada cambio de estado de un compromiso, con su autor.
create table seguimiento_compromisos (
    id_seguimiento bigint generated always as identity primary key,
    id_compromiso  bigint       not null references compromisos (id_compromiso),
    estado         varchar(20)  not null,
    observacion    varchar(500) not null,
    id_usuario     bigint       not null references usuarios (id_usuario),
    fecha          timestamp    not null default clock_timestamp()
);

create index ix_seguimiento_compromiso on seguimiento_compromisos (id_compromiso);
