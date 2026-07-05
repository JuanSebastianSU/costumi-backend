-- Módulo Pedidos/Carrito (RF-16): carrito persistente segmentado por (sucursal × cliente × tipo).
create table carrito (
    id          uuid        primary key,
    empresa_id  uuid        not null references empresa (id),
    sucursal_id uuid        not null references sucursal (id),
    cliente_id  uuid        not null references cliente (id),
    tipo        varchar(10) not null,
    estado      varchar(12) not null
);

-- Un único carrito PENDIENTE por (empresa × sucursal × cliente × tipo) — RF-16.2/16.3.
create unique index ux_carrito_pendiente
    on carrito (empresa_id, sucursal_id, cliente_id, tipo) where estado = 'PENDIENTE';
create index idx_carrito_cliente on carrito (empresa_id, cliente_id);

create table linea_de_carrito (
    id         uuid    primary key,
    carrito_id uuid    not null references carrito (id),
    empresa_id uuid    not null references empresa (id),
    prenda_id  uuid    not null references prenda (id),
    cantidad   integer not null
);

create index idx_linea_carrito on linea_de_carrito (carrito_id);
