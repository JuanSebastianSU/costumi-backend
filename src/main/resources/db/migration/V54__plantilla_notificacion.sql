-- Plantillas de mensajes automáticos por empresa (RF-11). Cada empresa personaliza el texto (con
-- variables entre llaves) y el switch on/off por tipo de evento. Las que no se personalizan usan su
-- default en memoria (no hay fila); por eso la unicidad es (empresa_id, tipo).
create table plantilla_notificacion (
    id         uuid          primary key,
    empresa_id uuid          not null references empresa (id),
    tipo       varchar(40)   not null,   -- MULTA_GENERADA | DEUDA_SALDADA | RENTA_CONFIRMADA | ...
    texto      varchar(1000) not null,
    activa     boolean       not null,
    constraint uq_plantilla_empresa_tipo unique (empresa_id, tipo)
);

create index idx_plantilla_notificacion_empresa on plantilla_notificacion (empresa_id);
