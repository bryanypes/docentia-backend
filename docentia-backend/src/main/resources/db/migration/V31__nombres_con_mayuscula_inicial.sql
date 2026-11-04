-- Nombres que llegaron todo en mayúsculas o todo en minúsculas (Campus, CSV) pasan a mayúscula
-- inicial, con los conectores en minúscula y los números romanos en mayúscula. Es la misma regla de
-- NombrePropio en Java; los nombres ya bien escritos no se tocan.
create function pg_temp.nombre_propio(texto text) returns text
    language plpgsql immutable as
$$
declare
    conectores text[] := array['a', 'al', 'con', 'de', 'del', 'e', 'el', 'en', 'la', 'las', 'los',
                                'o', 'para', 'por', 'u', 'y'];
    palabra    text;
    resultado  text := '';
    posicion   int  := 0;
begin
    foreach palabra in array regexp_split_to_array(btrim(regexp_replace(texto, '\s+', ' ', 'g')), ' ')
        loop
            posicion := posicion + 1;
            if posicion > 1 and lower(palabra) = any (conectores) then
                palabra := lower(palabra);
            elsif upper(palabra) ~ '^\(?(I|II|III|IV|V|VI|VII|VIII|IX|X)\)?$' then
                palabra := upper(palabra);
            else
                palabra := initcap(lower(palabra));
            end if;
            resultado := resultado || case when posicion > 1 then ' ' else '' end || palabra;
        end loop;
    return resultado;
end
$$;

update facultades
   set nombre = pg_temp.nombre_propio(nombre)
 where nombre ~ '[[:alpha:]]' and (nombre = upper(nombre) or nombre = lower(nombre));

update programas
   set nombre = pg_temp.nombre_propio(nombre)
 where nombre ~ '[[:alpha:]]' and (nombre = upper(nombre) or nombre = lower(nombre));

update cursos
   set nombre = pg_temp.nombre_propio(nombre)
 where nombre ~ '[[:alpha:]]' and (nombre = upper(nombre) or nombre = lower(nombre));

update usuarios
   set nombre_completo = pg_temp.nombre_propio(nombre_completo)
 where nombre_completo ~ '[[:alpha:]]'
   and (nombre_completo = upper(nombre_completo) or nombre_completo = lower(nombre_completo));
