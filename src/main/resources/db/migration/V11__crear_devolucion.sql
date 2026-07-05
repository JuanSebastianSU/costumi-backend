-- Módulo Devoluciones (RF-5): devolución de renta con liquidación del depósito y checklist.
create table devolucion (
    id                uuid           primary key,
    empresa_id        uuid           not null references empresa (id),
    renta_id          uuid           not null references renta (id),
    deposito          numeric(12, 2) not null,
    cargo_por_danos   numeric(12, 2) not null,
    cargo_por_retraso numeric(12, 2) not null,
    remanente         numeric(12, 2) not null
);

create index idx_devolucion_empresa on devolucion (empresa_id);
create index idx_devolucion_renta on devolucion (renta_id);

create table pieza_revisada (
    id            uuid         primary key,
    devolucion_id uuid         not null references devolucion (id),
    empresa_id    uuid         not null references empresa (id),
    descripcion   varchar(200) not null,
    llego         boolean      not null,
    estado        varchar(12)  not null
);

create index idx_pieza_devolucion on pieza_revisada (devolucion_id);
