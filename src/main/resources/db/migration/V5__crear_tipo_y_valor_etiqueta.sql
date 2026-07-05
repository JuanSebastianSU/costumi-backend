-- Módulo Catálogo y taxonomía (RF-2.7): motor de etiquetas (dimensiones y sus valores).
create table tipo_etiqueta (
    id                    uuid         primary key,
    empresa_id            uuid         not null references empresa (id),
    nombre                varchar(120) not null,
    define_variante       boolean      not null default false,
    seleccionable_cliente boolean      not null default false,
    archivada             boolean      not null default false
);

create index idx_tipo_etiqueta_empresa on tipo_etiqueta (empresa_id);
create unique index ux_tipo_etiqueta_empresa_nombre
    on tipo_etiqueta (empresa_id, lower(nombre)) where not archivada;

create table valor_etiqueta (
    id               uuid         primary key,
    empresa_id       uuid         not null references empresa (id),
    tipo_etiqueta_id uuid         not null references tipo_etiqueta (id),
    valor            varchar(120) not null,
    archivada        boolean      not null default false
);

create index idx_valor_etiqueta_tipo on valor_etiqueta (tipo_etiqueta_id);
create unique index ux_valor_etiqueta_tipo_valor
    on valor_etiqueta (tipo_etiqueta_id, lower(valor)) where not archivada;
