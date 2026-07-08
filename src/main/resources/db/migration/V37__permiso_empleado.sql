-- Permisos granulares por empleado (RF-1.5): overrides (casillas activadas/desactivadas por el dueño)
-- encima de la plantilla del rol. La ausencia de fila = se usa el valor por defecto del rol.
create table permiso_empleado (
    id         uuid        primary key,
    empresa_id uuid        not null references empresa (id),
    usuario_id uuid        not null references usuario (id),
    seccion    varchar(20) not null,
    accion     varchar(10) not null,
    concedido  boolean     not null
);

create unique index ux_permiso_empleado on permiso_empleado (usuario_id, seccion, accion);
create index idx_permiso_empleado_empresa on permiso_empleado (empresa_id, usuario_id);
