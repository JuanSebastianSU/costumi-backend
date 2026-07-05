-- Módulo Ventas/POS (RF-4): venta a nombre del empleado, con sus líneas.
create table venta (
    id          uuid           primary key,
    empresa_id  uuid           not null references empresa (id),
    sucursal_id uuid           not null references sucursal (id),
    empleado_id uuid           not null references usuario (id),
    cliente_id  uuid           references cliente (id),
    descuento   numeric(12, 2) not null,
    total       numeric(12, 2) not null,
    estado      varchar(12)    not null
);

create index idx_venta_empresa on venta (empresa_id);

create table linea_de_venta (
    id              uuid           primary key,
    venta_id        uuid           not null references venta (id),
    empresa_id      uuid           not null references empresa (id),
    prenda_id       uuid           not null references prenda (id),
    cantidad        integer        not null,
    precio_unitario numeric(12, 2) not null
);

create index idx_linea_venta on linea_de_venta (venta_id);
