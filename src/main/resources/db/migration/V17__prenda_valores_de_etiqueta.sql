-- Capa 2 (RF-2.7): la Prenda lleva sus valores de etiqueta (clasificación del ítem), una fila por
-- dimensión (tipo) y a lo sumo una vez por prenda. Solo referencias por id a la taxonomía (renombrar
-- un valor propaga sin tocar la prenda).
create table prenda_valor_etiqueta (
    prenda_id         uuid not null references prenda (id) on delete cascade,
    tipo_etiqueta_id  uuid not null references tipo_etiqueta (id),
    valor_etiqueta_id uuid not null references valor_etiqueta (id),
    primary key (prenda_id, tipo_etiqueta_id)
);

create index idx_prenda_valor_etiqueta_prenda on prenda_valor_etiqueta (prenda_id);
