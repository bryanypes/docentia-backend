-- Antes de migrar se apagan los disparadores que protegen las actas firmadas, sus firmas y sus
-- compromisos: algunas migraciones agregan columnas y les dan valor a las actas existentes (por
-- ejemplo V36, el tipo de acta, y V51, los datos congelados). Las migraciones no cambian notas,
-- hashes ni PDF; los disparadores vuelven en afterMigrate y en afterMigrateError. En una base nueva
-- aún no existen y no se hace nada.
do $$
declare
    t record;
begin
    for t in select c.relname as tabla, g.tgname as disparador
             from pg_trigger g join pg_class c on c.oid = g.tgrelid
             where g.tgname in ('tr_actas_inmutables', 'tr_firmas_acta_inmutables',
                                'tr_compromisos_acta_inmutables') loop
        execute format('alter table %I disable trigger %I', t.tabla, t.disparador);
    end loop;
end
$$;
