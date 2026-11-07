-- Agregar soporte para rol ADMINISTRADOR (solo lectura, sin alcance por facultad/programa)

alter table usuarios drop constraint ck_usuarios_rol;
alter table usuarios add constraint ck_usuarios_rol
    check (rol in ('COORDINADOR', 'VICERRECTOR', 'ADMINISTRADOR', 'DOCENTE', 'ESTUDIANTE'));

alter table usuarios drop constraint ck_usuarios_alcance;
alter table usuarios add constraint ck_usuarios_alcance check (
    (rol = 'COORDINADOR' and id_facultad is not null and id_programa is null)
    or (rol = 'DOCENTE' and id_programa is not null and id_facultad is null)
    or (rol = 'ESTUDIANTE' and id_programa is not null and id_facultad is null)
    or (rol = 'VICERRECTOR' and id_facultad is null and id_programa is null)
    or (rol = 'ADMINISTRADOR' and id_facultad is null and id_programa is null)
);
