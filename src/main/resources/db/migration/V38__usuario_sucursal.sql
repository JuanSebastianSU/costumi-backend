-- Empleado ligado a una o varias sucursales (RF-1.2/8.1): asignación de trabajo del personal.
create table usuario_sucursal (
    id          uuid primary key,
    empresa_id  uuid not null references empresa (id),
    usuario_id  uuid not null references usuario (id),
    sucursal_id uuid not null references sucursal (id)
);

create unique index ux_usuario_sucursal on usuario_sucursal (usuario_id, sucursal_id);
create index idx_usuario_sucursal_empresa on usuario_sucursal (empresa_id, usuario_id);
