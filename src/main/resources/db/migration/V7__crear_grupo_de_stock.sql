-- Módulo Inventario (RF-2.2): grupos de stock (variantes) con desglose de estado.
create table grupo_de_stock (
    id          uuid         primary key,
    empresa_id  uuid         not null references empresa (id),
    prenda_id   uuid         not null references prenda (id),
    etiqueta    varchar(160),
    disponibles integer      not null default 0,
    danadas     integer      not null default 0,
    en_limpieza integer      not null default 0,
    perdidas    integer      not null default 0
);

create index idx_grupo_stock_prenda on grupo_de_stock (prenda_id);
create index idx_grupo_stock_empresa on grupo_de_stock (empresa_id);
