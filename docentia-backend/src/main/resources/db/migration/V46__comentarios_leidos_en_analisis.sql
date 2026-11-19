-- Cuántos comentarios escritos había cuando se generó el análisis: si llegan más, la coordinación
-- ve el aviso de actualizarlo.
alter table solicitudes_analisis add column comentarios_leidos integer;
