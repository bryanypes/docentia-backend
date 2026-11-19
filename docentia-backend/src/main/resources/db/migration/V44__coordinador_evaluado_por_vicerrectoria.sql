-- El coordinador también es docente evaluado: su acta la firma la vicerrectoría en el lugar de la
-- coordinación. El evaluado firma siempre con el papel DOCENTE.
alter table firmas_acta drop constraint ck_firmas_rol;
alter table firmas_acta
    add constraint ck_firmas_rol check (rol in ('COORDINADOR', 'VICERRECTOR', 'DOCENTE'));
