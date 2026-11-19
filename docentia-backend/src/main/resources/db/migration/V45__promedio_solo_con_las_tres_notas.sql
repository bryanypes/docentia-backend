-- El promedio solo se calcula con las tres notas (estudiantes, coordinación o vicerrectoría y
-- autoevaluación). Un promedio PARCIAL ya no guarda valor ni categoría.
alter table promedios_docente drop constraint ck_promedios_coherencia;
update promedios_docente set valor = null, categoria = null where estado = 'PARCIAL';
alter table promedios_docente add constraint ck_promedios_coherencia check (
    (estado = 'COMPLETO' and valor between 0 and 5 and categoria is not null and fuentes_pendientes is null)
    or (estado = 'PARCIAL' and valor is null and categoria is null and fuentes_pendientes is not null)
    or (estado = 'SIN_NOTA' and valor is null and categoria is null)
);
