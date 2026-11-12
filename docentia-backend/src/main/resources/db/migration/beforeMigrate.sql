-- Antes de migrar se apaga el disparador que impide modificar actas firmadas o anuladas: algunas
-- migraciones agregan columnas y les dan valor a las actas existentes (por ejemplo V36, el tipo de
-- acta). Las migraciones no cambian notas, hashes ni PDF; el disparador vuelve en afterMigrate y en
-- afterMigrateError. En una base nueva la tabla aún no existe y no se hace nada.
do $$
begin
    if exists (select 1 from pg_trigger where tgname = 'tr_actas_inmutables') then
        alter table actas disable trigger tr_actas_inmutables;
    end if;
end
$$;
