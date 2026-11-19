-- Tutorial guiado del primer inicio de sesión: nadie lo ha visto todavía, así que todos los
-- usuarios lo verán la próxima vez que entren.
alter table usuarios add column tutorial_visto boolean not null default false;
