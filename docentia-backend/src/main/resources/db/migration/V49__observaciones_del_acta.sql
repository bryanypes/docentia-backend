-- Lo que se habló en la reunión de retroalimentación; se escribe antes de firmar y va en el PDF.
alter table actas add column observaciones varchar(2000);

-- Firmada, el acta tampoco cambia sus observaciones.
create or replace function proteger_acta_firmada() returns trigger as $$
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
        or new.pdf is distinct from old.pdf
        or new.hash_pdf is distinct from old.hash_pdf
        or new.hash_contenido is distinct from old.hash_contenido
        or new.codigo_verificacion is distinct from old.codigo_verificacion
        or new.promedio is distinct from old.promedio
        or new.categoria is distinct from old.categoria
        or new.fecha_firma is distinct from old.fecha_firma
        or new.observaciones is distinct from old.observaciones
    ) then
        raise exception 'Un acta firmada es inmutable';
    end if;
    return new;
end;
$$ language plpgsql;
