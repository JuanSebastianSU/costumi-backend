-- Módulo Notificaciones (RF-11): notificaciones a clientes por canal.
create table notificacion (
    id         uuid         primary key,
    empresa_id uuid         not null references empresa (id),
    cliente_id uuid         references cliente (id),
    canal      varchar(12)  not null,
    mensaje    varchar(500) not null,
    estado     varchar(12)  not null,
    fecha      timestamptz  not null
);

create index idx_notificacion_empresa on notificacion (empresa_id);
