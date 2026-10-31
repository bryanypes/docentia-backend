-- Cada cita queda marcada como verificada si su fragmento aparece en un comentario no filtrado del
-- docente y periodo. Un hallazgo sin ninguna cita verificada no se puede validar.
alter table citas_hallazgo add column verificada boolean not null default false;

alter table hallazgos_analisis drop constraint ck_hallazgos_estado;
alter table hallazgos_analisis add constraint ck_hallazgos_estado
    check (estado in ('PENDIENTE_VALIDACION', 'SIN_CITA_VERIFICADA', 'VALIDADO'));

-- Quién validó el hallazgo y cuándo (la validación completa del borrador es E4).
alter table hallazgos_analisis add column id_validador bigint references usuarios (id_usuario);
alter table hallazgos_analisis add column fecha_validacion timestamp;
