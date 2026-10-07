create table ponderaciones (
    id_ponderacion      bigint generated always as identity primary key,
    id_periodo          bigint        not null references periodos_academicos (id_periodo),
    peso_estudiantes    numeric(5, 2) not null,
    peso_coordinacion   numeric(5, 2) not null,
    peso_vicerrectoria  numeric(5, 2) not null,
    fecha_actualizacion timestamp     not null default now(),
    constraint uq_ponderaciones_periodo unique (id_periodo),
    constraint ck_ponderaciones_rango check (
        peso_estudiantes between 0 and 100
        and peso_coordinacion between 0 and 100
        and peso_vicerrectoria between 0 and 100
    ),
    constraint ck_ponderaciones_suma check (peso_estudiantes + peso_coordinacion + peso_vicerrectoria = 100)
);
