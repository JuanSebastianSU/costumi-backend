-- Módulo Inventario (RF-2): prendas (ítems concretos de la biblioteca).
create table prenda (
    id            uuid          primary key,
    empresa_id    uuid          not null references empresa (id),
    categoria_id  uuid          not null references categoria (id),
    nombre        varchar(160)  not null,
    tipo_articulo varchar(10)   not null,
    precio_renta  numeric(12, 2),
    precio_venta  numeric(12, 2),
    archivada     boolean       not null default false
);

create index idx_prenda_empresa on prenda (empresa_id);
create index idx_prenda_categoria on prenda (categoria_id);
