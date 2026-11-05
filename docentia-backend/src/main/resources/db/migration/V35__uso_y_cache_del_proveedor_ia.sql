-- Medición del gasto del proveedor externo y caché de sus respuestas. Ninguna de las dos tablas
-- guarda texto de comentarios ni datos de personas.

create table uso_ia (
    id                       bigint generated always as identity primary key,
    fecha                    timestamp    not null default now(),
    id_periodo               bigint       references periodos_academicos (id_periodo),
    proveedor                varchar(40)  not null,
    modelo                   varchar(100) not null,
    version_prompt           varchar(40)  not null,
    -- SHA-256 de lo que se envió (o se habría enviado); no permite reconstruir los comentarios
    huella                   char(64)     not null,
    comentarios_disponibles  int          not null,
    comentarios_enviados     int          not null,
    tokens_entrada           int          not null default 0,
    tokens_salida            int          not null default 0,
    tokens_cache             int          not null default 0,
    -- tokens de entrada que habría costado mandar todos los comentarios completos
    tokens_entrada_sin_topes int          not null default 0,
    estimado                 boolean      not null default false,
    resultado                varchar(30)  not null,
    duracion_ms              int          not null default 0,
    constraint ck_uso_ia_resultado
        check (resultado in ('LLAMADA', 'ACIERTO_CACHE', 'RECHAZADO_PRESUPUESTO', 'ERROR'))
);

create index ix_uso_ia_fecha on uso_ia (fecha);
create index ix_uso_ia_periodo on uso_ia (id_periodo, resultado);

-- Respuesta del proveedor por huella de entrada. La huella ya incluye modelo, versión del prompt y
-- topes, así que un cambio en cualquiera de ellos obliga a una llamada nueva.
create table cache_respuestas_ia (
    huella         char(64)     primary key,
    modelo         varchar(100) not null,
    version_prompt varchar(40)  not null,
    contenido      text         not null,
    tokens_entrada int          not null default 0,
    tokens_salida  int          not null default 0,
    creado_en      timestamp    not null default now()
);

alter table solicitudes_analisis
    -- LOCAL fuerza el modelo por reglas en esta solicitud; nulo usa el proveedor configurado
    add column modelo_solicitado varchar(10),
    add column motivo_regeneracion varchar(300),
    add constraint ck_solicitudes_modelo_solicitado
        check (modelo_solicitado is null or modelo_solicitado = 'LOCAL');
