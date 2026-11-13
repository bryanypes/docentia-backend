-- Solo para MigracionesActasFirmadasTest: una migración que falla a la mitad, después de un cambio,
-- para comprobar que afterMigrateError vuelve a encender la protección de las actas firmadas.
create table tabla_de_paso (id int);
select 1 / 0;
