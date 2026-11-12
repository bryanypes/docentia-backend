-- Si una migración falla, la protección de las actas firmadas vuelve a quedar encendida.
-- Vuelve a encender el disparador aunque el arranque se detenga (ver beforeMigrate.sql).
do $$
begin
    if exists (select 1 from pg_trigger where tgname = 'tr_actas_inmutables') then
        alter table actas enable trigger tr_actas_inmutables;
    end if;
end
$$;
