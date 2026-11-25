update actas a set facultad_id = coalesce(pr.id_facultad, u.id_facultad)
from usuarios u
left join programas pr on pr.id_programa = u.id_programa
where u.id_usuario = a.id_docente;
