-- Módulo Rentas (RF-3): rentas con fechas, importe, depósito y estado.
create table renta (
    id               uuid          primary key,
    empresa_id       uuid          not null references empresa (id),
    sucursal_id      uuid          not null references sucursal (id),
    cliente_id       uuid          not null references cliente (id),
    prenda_id        uuid          not null references prenda (id),
    fecha_retiro     date          not null,
    fecha_devolucion date          not null,
    precio_por_dia   numeric(12, 2) not null,
    deposito         numeric(12, 2) not null,
    importe          numeric(12, 2) not null,
    estado           varchar(12)   not null
);

create index idx_renta_empresa on renta (empresa_id);
create index idx_renta_cliente on renta (empresa_id, cliente_id);
