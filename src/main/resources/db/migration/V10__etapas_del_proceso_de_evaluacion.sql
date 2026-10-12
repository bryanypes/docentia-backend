-- Las etapas pasan a ser las cuatro del proceso: recolección de calificaciones, revisión de
-- hallazgos, agenda de citas y firma de actas. Las tres etapas de calificación anteriores se
-- funden en la recolección, con la ventana que va de la primera apertura al último cierre, y la
-- retroalimentación pasa a ser la revisión de hallazgos.

alter table etapas_periodo drop constraint ck_etapas_periodo_tipo;

insert into etapas_periodo (id_periodo, tipo_etapa, fecha_apertura, fecha_cierre)
select id_periodo, 'RECOLECCION_CALIFICACIONES', min(fecha_apertura), max(fecha_cierre)
from etapas_periodo
where tipo_etapa in ('EVALUACION_ESTUDIANTES', 'CALIFICACION_COORDINACION', 'CALIFICACION_VICERRECTORIA')
group by id_periodo;

delete from etapas_periodo
where tipo_etapa in ('EVALUACION_ESTUDIANTES', 'CALIFICACION_COORDINACION', 'CALIFICACION_VICERRECTORIA');

update etapas_periodo set tipo_etapa = 'REVISION_HALLAZGOS' where tipo_etapa = 'RETROALIMENTACION';

alter table etapas_periodo add constraint ck_etapas_periodo_tipo check (tipo_etapa in (
    'RECOLECCION_CALIFICACIONES',
    'REVISION_HALLAZGOS',
    'AGENDA_CITAS',
    'FIRMA_ACTAS'
));
