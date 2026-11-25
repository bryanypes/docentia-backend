-- La base protege el acta entera, no solo algunas columnas.
--
-- * Firmada: ninguna columna cambia; solo puede pasar a ANULADA (al corregirla).
-- * En borrador con una firma: lo que se firmó (resultado, datos congelados, tipo, observaciones)
--   ya no cambia; solo se cierra con la segunda firma (código, hashes, PDF, fecha y estado).
-- * Anulada: no cambia nada.
-- * Sus firmas y compromisos: no se agregan, quitan ni modifican con el acta firmada o anulada. Del
--   compromiso solo cambian después el estado y la observación del seguimiento.
create or replace function proteger_acta_firmada() returns trigger as $$
declare
    cierre constant text[] := array['estado', 'version_bloqueo', 'codigo_verificacion',
        'hash_contenido', 'hash_pdf', 'pdf', 'fecha_firma'];
begin
    if tg_op = 'DELETE' then
        if old.estado <> 'BORRADOR' then
            raise exception 'Un acta firmada no se puede eliminar';
        end if;
        return old;
    end if;
    if old.estado = 'ANULADA' then
        raise exception 'Un acta anulada no se puede modificar';
    end if;
    if old.estado = 'FIRMADA' and (
        new.estado <> 'ANULADA'
        or to_jsonb(new) - 'estado' - 'version_bloqueo'
           is distinct from to_jsonb(old) - 'estado' - 'version_bloqueo'
    ) then
        raise exception 'Un acta firmada es inmutable';
    end if;
    if old.estado = 'BORRADOR'
       and exists (select 1 from firmas_acta where id_acta = old.id_acta)
       and to_jsonb(new) - cierre is distinct from to_jsonb(old) - cierre then
        raise exception 'El acta ya tiene una firma: su contenido no cambia';
    end if;
    return new;
end;
$$ language plpgsql;

create function proteger_firmas_acta() returns trigger as $$
declare
    estado_acta varchar(20);
begin
    select estado into estado_acta from actas
     where id_acta = case when tg_op = 'INSERT' then new.id_acta else old.id_acta end;
    if estado_acta in ('FIRMADA', 'ANULADA') then
        raise exception 'Las firmas de un acta firmada no cambian';
    end if;
    if tg_op = 'UPDATE' then
        raise exception 'Una firma registrada no cambia';
    end if;
    return case when tg_op = 'DELETE' then old else new end;
end;
$$ language plpgsql;

create trigger tr_firmas_acta_inmutables before insert or update or delete on firmas_acta
    for each row execute function proteger_firmas_acta();

create function proteger_compromisos_acta() returns trigger as $$
declare
    acta_vieja varchar(20);
    acta_nueva varchar(20);
begin
    if tg_op <> 'INSERT' and old.id_acta is not null then
        select estado into acta_vieja from actas where id_acta = old.id_acta;
    end if;
    if tg_op <> 'DELETE' and new.id_acta is not null then
        select estado into acta_nueva from actas where id_acta = new.id_acta;
    end if;
    if tg_op = 'DELETE' then
        if acta_vieja in ('FIRMADA', 'ANULADA') then
            raise exception 'Un compromiso de un acta firmada no se elimina';
        end if;
        return old;
    end if;
    if tg_op = 'INSERT' then
        if acta_nueva in ('FIRMADA', 'ANULADA') then
            raise exception 'Un acta firmada no recibe compromisos nuevos';
        end if;
        return new;
    end if;
    -- Con el acta firmada solo se registra el seguimiento: estado y observación.
    if (acta_vieja in ('FIRMADA', 'ANULADA') or acta_nueva in ('FIRMADA', 'ANULADA'))
       and to_jsonb(new) - 'estado' - 'observacion'
           is distinct from to_jsonb(old) - 'estado' - 'observacion' then
        raise exception 'Un compromiso de un acta firmada solo cambia su seguimiento';
    end if;
    return new;
end;
$$ language plpgsql;

create trigger tr_compromisos_acta_inmutables before insert or update or delete on compromisos
    for each row execute function proteger_compromisos_acta();
