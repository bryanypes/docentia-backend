-- Cada solicitud puede pedir el modelo local (LOCAL) o el de lenguaje externo (LLM); nulo usa el
-- configurado por defecto.
alter table solicitudes_analisis drop constraint ck_solicitudes_modelo_solicitado;
alter table solicitudes_analisis
    add constraint ck_solicitudes_modelo_solicitado
        check (modelo_solicitado is null or modelo_solicitado in ('LOCAL', 'LLM'));
