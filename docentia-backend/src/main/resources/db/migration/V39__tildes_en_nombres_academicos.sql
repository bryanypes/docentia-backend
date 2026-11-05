-- Palabras académicas que Campus y los CSV guardan sin tilde ("Administracion", "Ingenieria").
-- Es la misma lista de NombrePropio en Java. Solo cambia la palabra completa, respetando si iba con
-- mayúscula inicial o en minúscula; un nombre bien escrito no contiene estas formas.
create temporary table tildes (sin text primary key, con text not null) on commit drop;

insert into tildes (sin, con)
values
       ('administracion', 'administración'),
       ('ingenieria', 'ingeniería'),
       ('educacion', 'educación'),
       ('comunicacion', 'comunicación'),
       ('informacion', 'información'),
       ('investigacion', 'investigación'),
       ('gestion', 'gestión'),
       ('electronica', 'electrónica'),
       ('mecanica', 'mecánica'),
       ('informatica', 'informática'),
       ('telematica', 'telemática'),
       ('tecnologia', 'tecnología'),
       ('tecnologica', 'tecnológica'),
       ('tecnologico', 'tecnológico'),
       ('tecnica', 'técnica'),
       ('tecnico', 'técnico'),
       ('psicologia', 'psicología'),
       ('pedagogia', 'pedagogía'),
       ('biologia', 'biología'),
       ('metodologia', 'metodología'),
       ('contaduria', 'contaduría'),
       ('economia', 'economía'),
       ('economicas', 'económicas'),
       ('juridicas', 'jurídicas'),
       ('politicas', 'políticas'),
       ('publica', 'pública'),
       ('publico', 'público'),
       ('basica', 'básica'),
       ('basicas', 'básicas'),
       ('matematica', 'matemática'),
       ('matematicas', 'matemáticas'),
       ('fisica', 'física'),
       ('quimica', 'química'),
       ('estadistica', 'estadística'),
       ('logistica', 'logística'),
       ('energetica', 'energética'),
       ('calculo', 'cálculo'),
       ('algebra', 'álgebra'),
       ('etica', 'ética'),
       ('analisis', 'análisis'),
       ('diseno', 'diseño'),
       ('produccion', 'producción'),
       ('construccion', 'construcción'),
       ('innovacion', 'innovación'),
       ('formacion', 'formación'),
       ('evaluacion', 'evaluación'),
       ('organizacion', 'organización'),
       ('direccion', 'dirección'),
       ('programacion', 'programación'),
       ('academica', 'académica'),
       ('academico', 'académico');

do
$$
    declare
        t record;
    begin
        for t in select sin, con from tildes
            loop
                update facultades set nombre = regexp_replace(nombre, '\m' || initcap(t.sin) || '\M', initcap(t.con), 'g')
                 where nombre ~ ('\m' || initcap(t.sin) || '\M');
                update facultades set nombre = regexp_replace(nombre, '\m' || t.sin || '\M', t.con, 'g')
                 where nombre ~ ('\m' || t.sin || '\M');
                update programas set nombre = regexp_replace(nombre, '\m' || initcap(t.sin) || '\M', initcap(t.con), 'g')
                 where nombre ~ ('\m' || initcap(t.sin) || '\M');
                update programas set nombre = regexp_replace(nombre, '\m' || t.sin || '\M', t.con, 'g')
                 where nombre ~ ('\m' || t.sin || '\M');
                update cursos set nombre = regexp_replace(nombre, '\m' || initcap(t.sin) || '\M', initcap(t.con), 'g')
                 where nombre ~ ('\m' || initcap(t.sin) || '\M');
                update cursos set nombre = regexp_replace(nombre, '\m' || t.sin || '\M', t.con, 'g')
                 where nombre ~ ('\m' || t.sin || '\M');
            end loop;
    end
$$;
