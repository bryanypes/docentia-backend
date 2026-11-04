-- Autoevaluación y coevaluación llevan un solo comentario general al final, como el formulario del
-- estudiante, y ese comentario entra al análisis de comentarios (fuente FORMULARIO). Los comentarios
-- por criterio anteriores se conservan en comentarios_criterio, pero la pantalla ya no los pide.
alter table evaluaciones_formulario add column comentario_general varchar(1000);
