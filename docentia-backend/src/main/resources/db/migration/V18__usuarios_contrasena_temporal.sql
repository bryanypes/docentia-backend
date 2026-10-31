-- La contraseña que asigna la vicerrectoría al crear o restablecer una cuenta es temporal: hasta
-- cambiarla, el usuario solo puede consultar su sesión y cambiarla. Las cuentas existentes no la
-- tienen marcada.
alter table usuarios
    add column debe_cambiar_contrasena boolean not null default false;
