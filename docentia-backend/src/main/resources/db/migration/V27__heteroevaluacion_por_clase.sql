-- Heteroevaluación: un formulario por (clase, docente, estudiante). AUTO y COEVAL siguen
-- uno por periodo. Los comentarios del formulario no requieren una importación CSV.
alter table evaluaciones_formulario drop constraint uq_evaluaciones_formulario;
alter table evaluaciones_formulario drop constraint ck_evaluaciones_tipo;

alter table evaluaciones_formulario
    add column clase_id bigint references clases (id);

alter table evaluaciones_formulario
    add constraint ck_evaluaciones_tipo check (tipo in ('AUTO', 'COEVAL', 'HETERO'));

alter table evaluaciones_formulario
    add constraint ck_evaluaciones_clase check (
        (tipo in ('AUTO', 'COEVAL') and clase_id is null)
        or (tipo = 'HETERO' and clase_id is not null)
    );

create unique index uq_evaluaciones_auto_coeval
    on evaluaciones_formulario (periodo_id, docente_id, tipo)
    where tipo in ('AUTO', 'COEVAL');

create unique index uq_evaluaciones_hetero
    on evaluaciones_formulario (clase_id, docente_id, evaluador_usuario_id)
    where tipo = 'HETERO';

create index ix_evaluaciones_clase on evaluaciones_formulario (clase_id);

alter table comentarios_estudiantes
    alter column id_importacion drop not null;
