-- Registro de auditoría. La base rechaza cualquier cambio: solo se inserta.
create table auditoria (
    id         bigint generated always as identity primary key,
    usuario_id bigint,
    correo     varchar(150),
    rol        varchar(20),
    accion     varchar(40)  not null,
    entidad    varchar(40),
    entidad_id bigint,
    fecha      timestamp    not null default clock_timestamp(),
    ip         varchar(64),
    resultado  varchar(20)  not null,
    constraint ck_auditoria_resultado check (resultado in ('EXITO', 'FALLO'))
);

create index ix_auditoria_fecha on auditoria (fecha desc);
create index ix_auditoria_accion on auditoria (accion, fecha desc);
create index ix_auditoria_usuario on auditoria (correo, fecha desc);

create function proteger_auditoria() returns trigger as $$
begin
    raise exception 'La auditoría es inmutable';
end;
$$ language plpgsql;

create trigger tr_auditoria_inmutable before update or delete on auditoria
    for each row execute function proteger_auditoria();
