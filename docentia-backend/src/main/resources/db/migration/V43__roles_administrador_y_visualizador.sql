-- Roles ADMINISTRADOR (usuarios, catálogos, ponderaciones y fechas) y VISUALIZADOR (paneles
-- agregados). Ninguno se vincula a una facultad ni a un programa. Si el PR #12 trae otra numeración
-- para su rol, se renumera allá.
alter table usuarios drop constraint ck_usuarios_rol;
alter table usuarios add constraint ck_usuarios_rol
    check (rol in ('COORDINADOR', 'VICERRECTOR', 'DOCENTE', 'ESTUDIANTE', 'ADMINISTRADOR', 'VISUALIZADOR'));

alter table usuarios drop constraint ck_usuarios_alcance;
alter table usuarios add constraint ck_usuarios_alcance check (
    (rol = 'COORDINADOR' and id_facultad is not null and id_programa is null)
    or (rol = 'DOCENTE' and id_programa is not null and id_facultad is null)
    or (rol = 'ESTUDIANTE' and id_programa is not null and id_facultad is null)
    or (rol in ('VICERRECTOR', 'ADMINISTRADOR', 'VISUALIZADOR') and id_facultad is null and id_programa is null)
);
