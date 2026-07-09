-- RF-8: baja/reactivación de empleados. Un usuario inactivo no puede autenticarse ni renovar sesión.
-- Default true: no cambia el comportamiento de los usuarios existentes.
alter table usuario add column activo boolean not null default true;
