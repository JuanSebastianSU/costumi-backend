-- Módulo Catálogo y taxonomía (RF-2.8): categorías ("parte del cuerpo") por empresa.
create table categoria (
    id         uuid         primary key,
    empresa_id uuid         not null references empresa (id),
    nombre     varchar(120) not null,
    archivada  boolean      not null default false
);

create index idx_categoria_empresa on categoria (empresa_id);

-- Nombre único por empresa entre las categorías activas (permite reusar el nombre tras archivar).
create unique index ux_categoria_empresa_nombre on categoria (empresa_id, lower(nombre)) where not archivada;
