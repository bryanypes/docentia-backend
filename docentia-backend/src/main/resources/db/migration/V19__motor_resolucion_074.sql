-- Resolución 074 de 2026: la tercera fuente es la autoevaluación del docente (20 %), un promedio con
-- fuentes faltantes se renormaliza y queda PARCIAL, y cada promedio guarda su categoría de desempeño.
alter table ponderaciones rename column peso_vicerrectoria to peso_autoevaluacion;
alter table ponderaciones alter column peso_estudiantes set default 50;
alter table ponderaciones alter column peso_coordinacion set default 30;
alter table ponderaciones alter column peso_autoevaluacion set default 20;

alter table calificaciones drop constraint ck_calificaciones_fuente;
update calificaciones set fuente = 'AUTOEVALUACION' where fuente = 'VICERRECTORIA';
alter table calificaciones add constraint ck_calificaciones_fuente
    check (fuente in ('ESTUDIANTES', 'COORDINACION', 'AUTOEVALUACION'));

alter table correcciones_calificacion drop constraint ck_correcciones_fuente;
alter table correcciones_calificacion drop constraint ck_correcciones_estados;
update correcciones_calificacion set fuente = 'AUTOEVALUACION' where fuente = 'VICERRECTORIA';
update correcciones_calificacion set estado_promedio_anterior = 'SIN_NOTA'
    where estado_promedio_anterior = 'INCOMPLETO';
update correcciones_calificacion set estado_promedio_nuevo = 'SIN_NOTA'
    where estado_promedio_nuevo = 'INCOMPLETO';
alter table correcciones_calificacion add constraint ck_correcciones_fuente
    check (fuente in ('ESTUDIANTES', 'COORDINACION', 'AUTOEVALUACION'));
alter table correcciones_calificacion add constraint ck_correcciones_estados check (
    estado_promedio_anterior in ('COMPLETO', 'PARCIAL', 'SIN_NOTA')
    and estado_promedio_nuevo in ('COMPLETO', 'PARCIAL', 'SIN_NOTA')
);

alter table promedios_docente drop constraint ck_promedios_estado;
alter table promedios_docente drop constraint ck_promedios_coherencia;
-- Sin valor guardado no se puede renormalizar aquí: al arrancar, la API recalcula las que faltan.
delete from promedios_docente where estado = 'INCOMPLETO';
alter table promedios_docente add column categoria varchar(20);
update promedios_docente set categoria = case
    when valor >= 4.60 then 'MUY_ALTO'
    when valor >= 3.80 then 'ALTO'
    when valor >= 3.00 then 'ACEPTABLE'
    when valor >= 2.20 then 'BAJO'
    else 'MUY_BAJO'
end;
alter table promedios_docente add constraint ck_promedios_estado
    check (estado in ('COMPLETO', 'PARCIAL', 'SIN_NOTA'));
alter table promedios_docente add constraint ck_promedios_categoria
    check (categoria in ('MUY_ALTO', 'ALTO', 'ACEPTABLE', 'BAJO', 'MUY_BAJO'));
alter table promedios_docente add constraint ck_promedios_coherencia check (
    (estado = 'COMPLETO' and valor between 0 and 5 and categoria is not null and fuentes_pendientes is null)
    or (estado = 'PARCIAL' and valor between 0 and 5 and categoria is not null and fuentes_pendientes is not null)
    or (estado = 'SIN_NOTA' and valor is null and categoria is null)
);
