-- La coordinación puede cancelar una cita o moverla a otra franja; la anterior queda liberada con
-- su motivo para la trazabilidad del proceso.
alter table citas drop constraint ck_citas_motivo;
alter table citas add constraint ck_citas_motivo check (
    motivo_liberacion is null or motivo_liberacion in (
        'REEMPLAZADA', 'LIBERADA_POR_DOCENTE', 'DECLINADA_POR_DOCENTE', 'INASISTENCIA',
        'CANCELADA_POR_COORDINACION', 'REPROGRAMADA_POR_COORDINACION'
    )
);
