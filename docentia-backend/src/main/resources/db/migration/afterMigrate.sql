-- Vuelve a encender la protección de las actas firmadas después de migrar (ver beforeMigrate.sql).
-- Corre en cada arranque, haya o no migraciones pendientes.
do $$
begin
    if to_regclass('actas') is not null
       and exists (select 1 from pg_trigger
                   where tgname = 'tr_actas_inmutables' and tgrelid = to_regclass('actas')) then
        alter table actas enable trigger tr_actas_inmutables;
    end if;
end
$$;
