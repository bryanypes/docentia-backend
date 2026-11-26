-- Si una migración falla, la protección de las actas firmadas vuelve a quedar encendida.
-- Vuelve a encender los disparadores aunque el arranque se detenga (ver beforeMigrate.sql).
do $$
declare
    t record;
begin
    for t in select c.relname as tabla, g.tgname as disparador
             from pg_trigger g join pg_class c on c.oid = g.tgrelid
             where g.tgname in ('tr_actas_inmutables', 'tr_firmas_acta_inmutables',
                                'tr_compromisos_acta_inmutables') loop
        execute format('alter table %I enable trigger %I', t.tabla, t.disparador);
    end loop;
end
$$;
