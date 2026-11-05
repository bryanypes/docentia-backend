-- Trazabilidad del proceso de evaluación: el docente confirma o declina su cita, la coordinación
-- registra si asistió y el acta dice si es de compromiso o de retroalimentación.

-- Confirmación del docente, normalmente el día anterior. DECLINADA libera la cita y la franja.
alter table citas add column confirmacion varchar(20);
alter table citas add column fecha_confirmacion timestamp;

-- Asistencia que registra la coordinación en la reunión. ASISTIO habilita el resultado al docente.
alter table citas add column asistencia varchar(20);
alter table citas add column fecha_asistencia timestamp;
alter table citas add column id_usuario_asistencia bigint references usuarios (id_usuario);

alter table citas add constraint ck_citas_confirmacion check (
    confirmacion is null or confirmacion in ('CONFIRMADA', 'DECLINADA')
);
alter table citas add constraint ck_citas_asistencia check (
    asistencia is null or asistencia in ('ASISTIO', 'NO_ASISTIO')
);

alter table citas drop constraint ck_citas_motivo;
alter table citas add constraint ck_citas_motivo check (
    motivo_liberacion is null or motivo_liberacion in (
        'REEMPLAZADA', 'LIBERADA_POR_DOCENTE', 'DECLINADA_POR_DOCENTE', 'INASISTENCIA'
    )
);

-- Tipo de acta. Las existentes toman el que corresponde a su nota frente al umbral.
alter table actas add column tipo varchar(20);
update actas
   set tipo = case
                  when promedio is not null and promedio < umbral_compromisos then 'COMPROMISO'
                  else 'RETROALIMENTACION'
              end;
alter table actas alter column tipo set not null;
alter table actas add constraint ck_actas_tipo check (tipo in ('COMPROMISO', 'RETROALIMENTACION'));
