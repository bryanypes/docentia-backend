-- Trazo de la firma hecho en la propia página: PNG que se imprime en el PDF, con su hash para
-- comprobar que no cambió.
alter table firmas_acta add column imagen bytea;
alter table firmas_acta add column hash_imagen varchar(64);
