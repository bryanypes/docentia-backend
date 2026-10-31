-- Bloqueo optimista. En actas, `version` ya es la versión del documento.
alter table actas add column version_bloqueo bigint not null default 0;
alter table hallazgos_analisis add column version_bloqueo bigint not null default 0;
alter table citas add column version_bloqueo bigint not null default 0;
alter table promedios_docente add column version_bloqueo bigint not null default 0;

-- Llaves foráneas que se filtran o se unen y no tenían índice.
create index ix_calificaciones_importacion on calificaciones (id_importacion);
create index ix_compromisos_periodo on compromisos (id_periodo);
create index ix_correcciones_calificacion_calificacion on correcciones_calificacion (id_calificacion);
create index ix_correcciones_calificacion_periodo on correcciones_calificacion (id_periodo);
create index ix_importaciones_usuario on importaciones (id_usuario);
create index ix_comentarios_estudiantes_periodo on comentarios_estudiantes (id_periodo);
create index ix_solicitudes_analisis_periodo on solicitudes_analisis (id_periodo);
create index ix_solicitudes_analisis_solicitante on solicitudes_analisis (id_solicitante);
create index ix_franjas_horarias_periodo on franjas_horarias (id_periodo);
create index ix_citas_periodo on citas (id_periodo);
create index ix_actas_periodo on actas (id_periodo);
create index ix_usuarios_facultad on usuarios (id_facultad);

-- Listados por estado de los promedios (I5 e I11).
create index ix_promedios_periodo_estado on promedios_docente (id_periodo, estado);
