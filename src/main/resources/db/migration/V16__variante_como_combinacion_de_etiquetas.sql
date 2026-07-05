-- Motor de variantes real (RF-2.7.3/2.7.4): el grupo de stock deja de tener una "etiqueta" suelta
-- y pasa a definirse por la COMBINACIÓN de valores de etiqueta. La combinación vive en una tabla
-- hija (una fila por dimensión), con una dimensión (tipo) a lo sumo una vez por grupo.
alter table grupo_de_stock drop column etiqueta;

create table grupo_de_stock_valor (
    grupo_id          uuid not null references grupo_de_stock (id) on delete cascade,
    tipo_etiqueta_id  uuid not null references tipo_etiqueta (id),
    valor_etiqueta_id uuid not null references valor_etiqueta (id),
    primary key (grupo_id, tipo_etiqueta_id)
);

create index idx_grupo_stock_valor_grupo on grupo_de_stock_valor (grupo_id);
