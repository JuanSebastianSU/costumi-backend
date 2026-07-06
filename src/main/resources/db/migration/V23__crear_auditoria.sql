-- Auditoría (RF-0.5/15.5): constancia de acciones relevantes por empresa, alimentada por domain events.
create table registro_auditoria (
    id         uuid         primary key,
    empresa_id uuid         not null references empresa (id),
    accion     varchar(60)  not null,
    detalle    varchar(500),
    fecha      timestamptz  not null
);

create index idx_auditoria_empresa on registro_auditoria (empresa_id);
