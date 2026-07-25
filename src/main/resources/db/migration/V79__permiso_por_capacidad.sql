-- Fase B (paso 5): los overrides de permisos pasan de (seccion, accion) a una CAPACIDAD fina del catálogo
-- (PLAN_PERMISOS_CATALOGO.md). Se recrea la tabla: la matriz granular estaba desconectada de la app (no había
-- overrides "en uso"), y el modelo viejo (seccion×accion) no mapea 1:1 al nuevo, así que se parte de cero.
drop table if exists permiso_empleado;

create table permiso_empleado (
    id         uuid         primary key,
    empresa_id uuid         not null references empresa (id),
    usuario_id uuid         not null references usuario (id),
    capacidad  varchar(50)  not null,
    concedido  boolean      not null
);
create unique index ux_permiso_empleado on permiso_empleado (usuario_id, capacidad);
create index idx_permiso_empleado_empresa on permiso_empleado (empresa_id, usuario_id);
